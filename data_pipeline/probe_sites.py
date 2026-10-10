"""Checks which recipe-scrapers sites we can actually scrape, before anyone collects URLs.

For each site in the list it finds one recipe page and tries it the way scrape_recipes.py
would, then records what happened in logs/site_probe.csv:

    python probe_sites.py supported_sites.txt                # every site (resumes where it stopped)
    python probe_sites.py supported_sites.txt --only budgetbytes.com,allrecipes.com
    python probe_sites.py supported_sites.txt --retry        # probe the non-ok sites again
    python probe_sites.py supported_sites.txt --retry --status no-recipe,no-urls
    python probe_sites.py supported_sites.txt --workers 4

Per site, at most 10 requests, one at a time, 3-5 s apart, with our User-Agent:
  1. robots.txt: if it disallows us (or answers 401/403), the site is marked "robots" and
     nothing else is fetched.
  2. Up to 5 sitemaps (the ones robots.txt names, then /sitemap.xml and the usual WordPress
     names), skipping page, category and tag sitemaps: URLs that look like single recipes,
     not roundups ("party-food-recipes", "top-25-..."), spread across the sitemap.
  3. Up to 4 of those pages, until one scrapes with a title and 2+ ingredients.

Different sites are probed in parallel (--workers, default 6); a single site never gets
more than one request at a time. A bot check or a block is recorded, never worked around.

Statuses:
  ok          a recipe page scraped with ingredients: collect URLs from this site
  robots      robots.txt disallows our bot
  blocked     HTTP 401, 403 or 429 on a recipe page
  challenge   a bot check page (Cloudflare, captcha, Akamai, DataDome...)
  no-recipe   the page loaded, but no ingredients came out
  no-urls     no sitemap with recipe-looking URLs; check the site by hand
  error       timeouts, DNS, server errors

The `lang` column is the page's <html lang>, so English sites can be filtered for.
"""
import argparse
import csv
import random
import re
import sys
import time
import zlib
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from urllib.parse import urlparse
from urllib.robotparser import RobotFileParser

import requests
from recipe_scrapers import scrape_html

from common import LOG_DIR, user_agent

RESULTS_PATH = LOG_DIR / "site_probe.csv"
USABLE_PATH = LOG_DIR / "usable_sites.txt"
FIELDS = ["site", "status", "detail", "lang",
          "ingredients", "title", "test_url", "checked_at"]

MIN_DELAY, MAX_DELAY = 3.0, 5.0     # seconds between requests to the same site
TIMEOUT = 20
MAX_BYTES = 5_000_000               # sitemaps can be huge; read at most this much
RECIPES_TO_TRY = 4
CANDIDATES = 8
SITEMAPS_TO_READ = 5
FALLBACK_SITEMAPS = ["/sitemap.xml", "/sitemap_index.xml", "/wp-sitemap.xml", "/post-sitemap.xml",
                     "/recipe-sitemap.xml", "/sitemap-index.xml"]

# Pages about many recipes, which hold no recipe schema of their own: "party-food-recipes",
# "best-frozen-pizza", "top-25-skinny-recipes-2012", "new-york-food-map"
ROUNDUP = re.compile(
    r"(^|[-/])(recipes|ideas|best|top|roundup|round-up|collection|guide|map|menu|meal-plan|"
    r"what-to|how-to-(?!make)|things|ways|favorites|gift|review|vs|\d{1,3})([-/]|$)", re.I)
# Child sitemaps that hold pages, categories or tags rather than recipes
NOT_RECIPE_SITEMAP = re.compile(r"(page|category|categories|tag|author|attachment|image|video|"
                                r"web-stor|product|collection)[-_]?sitemap|sitemap[-_]?(page|category|tag|author)",
                                re.I)

# Pages that aren't one recipe
NOT_A_RECIPE = re.compile(
    r"/(category|categories|tag|tags|page|author|about|contact|privacy|terms|search|shop|collections?|"
    r"wp-content|feed|videos?|cookbooks?|newsletter|login|account)(/|$)", re.I)
# A bot check page rather than the site itself
CHALLENGE = re.compile(
    r"just a moment\.\.\.|cf-chl|challenge-platform|captcha|px-captcha|datadome|"
    r"access denied|request unsuccessful|incapsula|are you a robot|verify you are human", re.I)


class Site:
    """One site's requests: one at a time, 3-5 s apart, as scrape_recipes.py does."""

    def __init__(self, host):
        self.host = host
        self.base = f"https://{host}"
        self.last = 0.0
        self.requests = 0
        self.robots = None

    def get(self, url, *, limit=MAX_BYTES):
        wait = random.uniform(MIN_DELAY, MAX_DELAY) - \
            (time.monotonic() - self.last)
        if self.last and wait > 0:
            time.sleep(wait)
        self.last = time.monotonic()
        self.requests += 1
        resp = requests.get(
            url, headers={"User-Agent": user_agent()}, timeout=TIMEOUT, stream=True)
        body = b""
        for chunk in resp.iter_content(64 * 1024):
            body += chunk
            if len(body) >= limit:
                break
        resp.close()
        return resp, body

    def allowed(self, url):
        return self.robots is None or self.robots.can_fetch(user_agent(), url)


# ---------- steps ----------

def read_robots(site):
    """None = allowed to continue; otherwise the reason we stop."""
    parser = RobotFileParser(f"{site.base}/robots.txt")
    resp, body = site.get(f"{site.base}/robots.txt")
    if resp.status_code in (401, 403):
        return f"robots.txt answered {resp.status_code}"
    if resp.status_code >= 400:
        parser.allow_all = True
    else:
        parser.parse(body.decode("utf-8", "replace").splitlines())
    site.robots = parser
    if not parser.can_fetch(user_agent(), f"{site.base}/"):
        return "robots.txt disallows our bot"
    return None


def sitemap_urls(site):
    named = site.robots.site_maps() if site.robots else None
    return list(named or []) + [site.base + path for path in FALLBACK_SITEMAPS]


LOC = re.compile(r"<(?:\w+:)?loc>\s*(?:<!\[CDATA\[)?\s*([^<\]\s]+)", re.I)


def parse_sitemap(body, url):
    """("sitemapindex" or "urlset", the <loc> URLs). Reads with a regex rather than an XML
    parser, because a big sitemap is cut off at MAX_BYTES: the URLs before the cut still
    count. A gzipped one is unpacked as far as it got."""
    if url.endswith(".gz") or body[:2] == b"\x1f\x8b":
        # tolerates a cut-off file
        body = zlib.decompressobj(16 + zlib.MAX_WBITS).decompress(body)
    text = body.decode("utf-8", "replace")
    tag = "sitemapindex" if re.search(
        r"<(?:\w+:)?sitemapindex[\s>]", text[:2000]) else "urlset"
    return tag, [m.group(1) for m in LOC.finditer(text)]


def looks_like_recipe(url, host):
    parsed = urlparse(url)
    if parsed.netloc.replace("www.", "") != host.replace("www.", ""):
        return False
    path = parsed.path.rstrip("/")
    if not path or path.count("/") > 4 or NOT_A_RECIPE.search(parsed.path):
        return False
    return not re.search(r"\.(jpg|jpeg|png|webp|gif|pdf|xml)$", path, re.I)


def find_candidates(site):
    """Recipe-looking URLs from the site's sitemaps (reads at most SITEMAPS_TO_READ).
    Single recipes come before roundups, spread across the sitemap rather than its first
    entries (often the oldest posts or the hub pages)."""
    seen, fetched = set(), 0
    queue = sitemap_urls(site)
    while queue and fetched < SITEMAPS_TO_READ:
        url = queue.pop(0)
        if url in seen or not site.allowed(url):
            continue
        seen.add(url)
        try:
            resp, body = site.get(url)
            fetched += 1
            if resp.status_code != 200:
                continue
            tag, locs = parse_sitemap(body, url)
        except (requests.RequestException, zlib.error, OSError, ValueError):
            continue
        if tag == "sitemapindex":
            # child sitemaps: recipe or post sitemaps first
            children = [
                u for u in locs if not NOT_RECIPE_SITEMAP.search(u)] or locs
            ranked = sorted(children, key=lambda u: (not re.search(r"recipe", u, re.I),
                                                     not re.search(r"post", u, re.I)))
            queue = ranked[:3] + queue
            continue
        pages = [u for u in locs if looks_like_recipe(
            u, site.host) and site.allowed(u)]
        singles = [u for u in pages if not ROUNDUP.search(
            urlparse(u).path.rstrip("/").rsplit("/", 1)[-1])]
        pool = singles or pages
        if pool:
            return spread(pool, CANDIDATES)
    return []


def spread(urls, n):
    """n URLs evenly spaced through the list, starting a little way in."""
    if len(urls) <= n:
        return urls
    step = len(urls) / (n + 1)
    return [urls[int(step * (i + 1))] for i in range(n)]


def html_lang(html):
    match = re.search(r"<html[^>]*\blang=[\"']?([a-zA-Z-]+)", html[:5000])
    return match.group(1).lower() if match else ""


def try_recipe(site, url):
    """(status, detail, lang, ingredients, title)."""
    resp, body = site.get(url, limit=10_000_000)
    html = body.decode(resp.encoding or "utf-8", "replace")
    if resp.status_code in (401, 403, 429):
        if CHALLENGE.search(html[:20000]) or resp.headers.get("cf-mitigated"):
            return "challenge", f"HTTP {resp.status_code} bot check", "", 0, ""
        return "blocked", f"HTTP {resp.status_code}", "", 0, ""
    if resp.status_code >= 400:
        return "error", f"HTTP {resp.status_code}", "", 0, ""
    if resp.headers.get("cf-mitigated") or (len(html) < 30000 and CHALLENGE.search(html)):
        return "challenge", "bot check page", html_lang(html), 0, ""
    lang = html_lang(html)
    try:
        scraper = scrape_html(html, org_url=url, supported_only=False)
        ingredients = scraper.ingredients() or []
        title = scraper.title() or ""
    except Exception as e:     # recipe-scrapers raises many kinds when a field is missing
        return "no-recipe", type(e).__name__, lang, 0, ""
    if len(ingredients) >= 2 and title:
        return "ok", "", lang, len(ingredients), title
    return "no-recipe", f"{len(ingredients)} ingredients", lang, len(ingredients), title


def probe(host):
    site = Site(host)
    row = {"site": host, "status": "", "detail": "", "lang": "", "ingredients": 0, "title": "",
           "test_url": "", "checked_at": datetime.now().isoformat(timespec="seconds")}
    try:
        stop = read_robots(site)
        if stop:
            row.update(status="robots", detail=stop)
            return row
        candidates = find_candidates(site)
        if not candidates:
            row.update(status="no-urls",
                       detail="no sitemap with recipe-looking URLs")
            return row
        for url in candidates[:RECIPES_TO_TRY]:
            status, detail, lang, count, title = try_recipe(site, url)
            row.update(status=status, detail=detail, lang=lang,
                       ingredients=count, title=title[:80], test_url=url)
            if status in ("ok", "blocked", "challenge"):
                break       # a block won't change on the next page; don't keep knocking
    except requests.RequestException as e:
        row.update(status="error", detail=type(e).__name__)
    except Exception as e:     # anything else odd about one site must not stop the run
        row.update(status="error", detail=f"{type(e).__name__}: {e}"[:120])
    return row


# ---------- files ----------

def read_sites(path):
    with open(path, encoding="utf-8") as f:
        lines = [line.strip().lower() for line in f]
    hosts = [line for line in lines if line and not line.startswith(
        "#") and "." in line and " " not in line]
    return list(dict.fromkeys(hosts))


def read_results():
    if not RESULTS_PATH.exists():
        return {}
    with open(RESULTS_PATH, newline="", encoding="utf-8") as f:
        return {row["site"]: row for row in csv.DictReader(f)}


def write_results(results):
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    rows = sorted(results.values(), key=lambda r: (
        r["status"] != "ok", r["site"]))
    with open(RESULTS_PATH, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    usable = sorted(r["site"] for r in rows if r["status"] == "ok")
    USABLE_PATH.write_text("\n".join(usable) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("sites", help="text file of site hosts, one per line")
    parser.add_argument(
        "--only", help="comma-separated hosts to probe (again)")
    parser.add_argument("--retry", action="store_true",
                        help="probe sites whose last status wasn't ok again")
    parser.add_argument(
        "--status", help="with --retry: only these statuses, comma-separated (e.g. no-recipe,no-urls)")
    parser.add_argument("--workers", type=int, default=6,
                        help="sites probed at once (default 6)")
    args = parser.parse_args()

    hosts = read_sites(args.sites)
    results = read_results()
    if args.only:
        targets = [h.strip().lower()
                   for h in args.only.split(",") if h.strip()]
    elif args.retry:
        wanted = {x.strip()
                  for x in args.status.split(",")} if args.status else None
        targets = [h for h in hosts if h in results and results[h]["status"] != "ok"
                   and (wanted is None or results[h]["status"] in wanted)]
    else:
        targets = [h for h in hosts if h not in results]     # resume
    if not targets:
        print(
            f"Nothing to probe: all {len(hosts)} sites have a result in {RESULTS_PATH}. Use --retry or --only.")
        return
    print(f"Probing {len(targets)} of {len(hosts)} sites with {args.workers} workers "
          f"(about {len(targets) * 20 // args.workers // 60 + 1} min)...")

    done = 0
    pool = ThreadPoolExecutor(max_workers=max(1, args.workers))
    futures = {pool.submit(probe, host): host for host in targets}
    try:
        for future in as_completed(futures):
            row = future.result()
            done += 1
            results[row["site"]] = row
            if done % 10 == 0:
                # saved as it goes, so stopping loses little
                write_results(results)
            extra = f" ({row['detail']})" if row["detail"] else ""
            print(
                f"  [{done}/{len(targets)}] {row['status']:<9} {row['site']}{extra}")
    except KeyboardInterrupt:
        pool.shutdown(wait=False, cancel_futures=True)
        write_results(results)
        sys.exit(
            "\nStopped. Results so far are saved; run the same command to carry on.")
    pool.shutdown()
    write_results(results)

    counts = {}
    for row in results.values():
        counts[row["status"]] = counts.get(row["status"], 0) + 1
    print("\n" + ", ".join(f"{k}: {v}" for k,
          v in sorted(counts.items(), key=lambda kv: -kv[1])))
    english = sum(1 for r in results.values()
                  if r["status"] == "ok" and r["lang"].startswith("en"))
    print(
        f"Usable: {counts.get('ok', 0)} sites ({english} in English) -> {USABLE_PATH}")
    print(f"Every result: {RESULTS_PATH}")


if __name__ == "__main__":
    main()
