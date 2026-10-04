"""Scrape recipe pages listed in urls.txt and send them to the backend.

For each URL: check robots.txt, fetch politely (one request at a time, 3-5 s apart per
site, identified by our User-Agent), cache the HTML, run recipe-scrapers on it, and
collect the raw fields. Recipes are posted in batches of 25 to
POST /api/admin/recipes/ingest, which skips URLs it already has.

Cached pages are never downloaded again, so re-runs (for example after a parser fix in
the backend) cost the sites nothing.

Usage (from data_pipeline/, venv active, backend running):
    python scrape_recipes.py                 # scrape urls.txt and post
    python scrape_recipes.py my_urls.txt     # a different URL list
    python scrape_recipes.py --dry-run       # scrape and print one payload, post nothing
"""
import hashlib
import json
import random
import sys
import time
from datetime import datetime
from pathlib import Path
from urllib.parse import urldefrag, urlparse
from urllib.robotparser import RobotFileParser

import requests
from recipe_scrapers import scrape_html

from common import CACHE_DIR, LOG_DIR, PIPELINE_DIR, post_json, user_agent

HTML_CACHE = Path(CACHE_DIR) / "html"
BATCH_SIZE = 25
MIN_DELAY, MAX_DELAY = 3.0, 5.0     # seconds between requests to the same site
TIMEOUT = 30

# host -> RobotFileParser, or None if robots.txt said "disallow everything"
_robots = {}
_last_request = {}    # host -> time of our last network request to it


# ---------- URLs ----------

def read_urls(path):
    """One URL per line; blank lines and '#' comments are ignored; duplicates dropped."""
    urls = []
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        url = urldefrag(line)[0]
        if url not in urls:
            urls.append(url)
    return urls


def host_of(url):
    host = (urlparse(url).hostname or "").lower()
    return host[4:] if host.startswith("www.") else host


# ---------- politeness ----------

def wait_for_turn(url):
    """Sleep so requests to the same site are 3-5 s apart (or the site's Crawl-delay, if longer)."""
    host = urlparse(url).netloc
    delay = random.uniform(MIN_DELAY, MAX_DELAY)
    robots = _robots.get(host)
    crawl_delay = robots.crawl_delay(user_agent()) if robots else None
    if crawl_delay:
        delay = max(delay, float(crawl_delay))

    elapsed = time.monotonic() - _last_request.get(host, 0)
    if elapsed < delay:
        time.sleep(delay - elapsed)
    _last_request[host] = time.monotonic()


def allowed_by_robots(url):
    """Fetch and cache each site's robots.txt once. 404 means everything is allowed;
    401/403 or an unreachable robots.txt means we stay away."""
    parsed = urlparse(url)
    host = parsed.netloc
    if host not in _robots:
        robots_url = f"{parsed.scheme}://{host}/robots.txt"
        parser = RobotFileParser(robots_url)
        try:
            wait_for_turn(robots_url)
            resp = requests.get(robots_url, headers={
                                "User-Agent": user_agent()}, timeout=TIMEOUT)
            if resp.status_code in (401, 403):
                parser.disallow_all = True
            elif resp.status_code >= 400:
                parser.allow_all = True        # no robots.txt: nothing is disallowed
            else:
                parser.parse(resp.text.splitlines())
        except requests.RequestException:
            parser.disallow_all = True         # can't check, so don't crawl
        _robots[host] = parser
    return _robots[host].can_fetch(user_agent(), url)


# ---------- fetching ----------

def cache_path(url):
    return HTML_CACHE / (hashlib.sha1(url.encode("utf-8")).hexdigest() + ".html")


def get_html(url):
    """Return (html, source) where source is 'cache' or 'network'. Raises on HTTP errors."""
    path = cache_path(url)
    if path.exists():
        return path.read_text(encoding="utf-8"), "cache"

    wait_for_turn(url)
    resp = requests.get(
        url, headers={"User-Agent": user_agent()}, timeout=TIMEOUT)
    if resp.status_code != 200:
        raise RuntimeError(f"HTTP {resp.status_code}")
    HTML_CACHE.mkdir(parents=True, exist_ok=True)
    path.write_text(resp.text, encoding="utf-8")
    return resp.text, "network"


# ---------- scraping ----------

def safe(fn):
    """recipe-scrapers raises when a field is missing; treat that (and empty values) as None."""
    try:
        value = fn()
    except Exception:
        return None
    if value in ("", [], None):
        return None
    return value


def minutes(value):
    return value if isinstance(value, int) and value >= 0 else None


def scrape(url, html):
    scraper = scrape_html(html, org_url=url, supported_only=False)
    keywords = safe(scraper.keywords)
    if isinstance(keywords, str):
        keywords = [k.strip() for k in keywords.split(",") if k.strip()]

    return {
        "sourceUrl": url,
        "host": host_of(url),
        "name": safe(scraper.title),
        "description": safe(scraper.description),
        "prepTimeMinutes": minutes(safe(scraper.prep_time)),
        "cookTimeMinutes": minutes(safe(scraper.cook_time)),
        "yields": safe(scraper.yields),
        "rawIngredients": safe(scraper.ingredients),
        "rawInstructions": safe(scraper.instructions_list),
        "imageUrl": safe(scraper.image),
        "category": safe(scraper.category),
        "cuisine": safe(scraper.cuisine),
        "keywords": keywords,
    }


# ---------- main ----------

def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    dry_run = "--dry-run" in sys.argv
    urls_path = Path(args[0]) if args else PIPELINE_DIR / "urls.txt"
    urls = read_urls(urls_path)
    print(f"{len(urls)} URLs from {urls_path.name}")

    results = []      # one entry per URL, written to the log
    recipes = []
    for i, url in enumerate(urls, start=1):
        entry = {"url": url}
        try:
            if not allowed_by_robots(url):
                entry["status"] = "blocked by robots.txt"
            else:
                html, source = get_html(url)
                recipe = scrape(url, html)
                if not recipe["name"] or not recipe["rawIngredients"]:
                    entry["status"] = f"no recipe found ({source})"
                else:
                    recipes.append(recipe)
                    entry["status"] = f"scraped ({source})"
        except Exception as e:   # one bad page never stops the run
            entry["status"] = f"error: {e}"
        results.append(entry)
        print(f"  [{i}/{len(urls)}] {entry['status']}: {url}")

    if dry_run:
        if recipes:
            print(json.dumps(recipes[0], indent=2, ensure_ascii=False))
        print(f"\nDry run: {len(recipes)} recipes scraped, nothing posted.")
        return

    totals = {"created": 0, "skippedDuplicate": 0,
              "needsReview": 0, "failed": []}
    for start in range(0, len(recipes), BATCH_SIZE):
        batch = recipes[start:start + BATCH_SIZE]
        result = post_json("/api/admin/recipes/ingest", {"recipes": batch})
        for key in ("created", "skippedDuplicate", "needsReview"):
            totals[key] += result[key]
        totals["failed"].extend(result["failed"])
        print(f"  batch {start // BATCH_SIZE + 1}: created {result['created']}, "
              f"skipped {result['skippedDuplicate']}, failed {len(result['failed'])}, "
              f"lines needing review {result['needsReview']}")

    LOG_DIR.mkdir(parents=True, exist_ok=True)
    log_path = LOG_DIR / f"scrape_{datetime.now():%Y%m%d_%H%M%S}.json"
    log_path.write_text(json.dumps(
        {"urls": results, "backend": totals}, indent=2), encoding="utf-8")

    print(f"\nDone: {totals['created']} created, {totals['skippedDuplicate']} already had, "
          f"{len(totals['failed'])} failed, {totals['needsReview']} lines need review.")
    for failure in totals["failed"]:
        print(f"  failed: {failure['sourceUrl']}: {failure['reason']}")
    print(f"Log: {log_path}")


if __name__ == "__main__":
    main()
