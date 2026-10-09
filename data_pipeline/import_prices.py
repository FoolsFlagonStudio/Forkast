"""Import ingredient prices into the backend.

    python import_prices.py bls             # latest BLS average prices for bls_series.csv
    python import_prices.py seed            # hand-entered prices from prices_seed.csv
    python import_prices.py bls --dry-run   # show what would be sent, post nothing

Both post to POST /api/admin/prices/import, which converts each price to a price per
100 g (and per item where it can), stores it, and then recalculates every recipe's cost.
Rerunning is safe: the backend skips a BLS month it already has and a seed price that
hasn't changed.

BLS: the US Bureau of Labor Statistics publishes monthly US city-average prices for about
70 staples. Each price is dated at the end of its month, so the backend's 60-day window
keeps the newest month current until the next one is published. BLS_API_KEY in .env is
optional; without it the public limits are 25 series per request and 25 requests a day.

Usage: from data_pipeline/, venv active, backend running.
"""
import argparse
import csv
import json
import os
import sys
from datetime import date, datetime, timezone
from pathlib import Path

import requests

from common import CACHE_DIR, PIPELINE_DIR, post_json

BLS_URL_V1 = "https://api.bls.gov/publicAPI/v1/timeseries/data/"
BLS_URL_V2 = "https://api.bls.gov/publicAPI/v2/timeseries/data/"
BLS_SERIES_PATH = PIPELINE_DIR / "bls_series.csv"
SEED_PRICES_PATH = PIPELINE_DIR / "prices_seed.csv"
CATALOG_PATH = PIPELINE_DIR / "ingredients_seed.csv"
BLS_CACHE = Path(CACHE_DIR) / "bls"
IMPORT_BATCH_SIZE = 200   # the backend's limit per call
BLS_STORE_NAME = "BLS U.S. city average"


# ---------- catalog check ----------

def catalog_names():
    """Every ingredient name and alias, lowercase, so typos are caught before posting."""
    names = set()
    with open(CATALOG_PATH, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            names.add(row["name"].strip().lower())
            for alias in (row.get("aliases") or "").split("|"):
                if alias.strip():
                    names.add(alias.strip().lower())
    return names


def check_names(rows, source_file):
    known = catalog_names()
    unknown = sorted({r["ingredient"] for r in rows if r["ingredient"].strip().lower() not in known})
    if unknown:
        sys.exit(f"{source_file.name} has ingredients that aren't in the catalog: {', '.join(unknown)}")


# ---------- BLS ----------

def read_bls_series():
    with open(BLS_SERIES_PATH, newline="", encoding="utf-8") as f:
        rows = [r for r in csv.DictReader(f) if r["ingredient"].strip()]
    check_names(rows, BLS_SERIES_PATH)
    return rows


def fetch_bls(series_ids):
    """Return {series_id: (year, month, value)} for the newest monthly value of each series."""
    api_key = os.getenv("BLS_API_KEY", "").strip()
    url, per_request = (BLS_URL_V2, 50) if api_key else (BLS_URL_V1, 25)
    this_year = date.today().year

    latest, messages, raw = {}, [], []
    for start in range(0, len(series_ids), per_request):
        body = {
            "seriesid": series_ids[start:start + per_request],
            "startyear": str(this_year - 1),
            "endyear": str(this_year),
        }
        if api_key:
            body["registrationkey"] = api_key   # sent in the body, never in a URL or log
        resp = requests.post(url, json=body, timeout=60)
        resp.raise_for_status()
        payload = resp.json()
        raw.append(payload)
        if payload.get("status") != "REQUEST_SUCCEEDED":
            sys.exit(f"BLS request failed: {payload.get('status')} {payload.get('message')}")
        messages.extend(payload.get("message") or [])

        for series in payload["Results"]["series"]:
            for point in series.get("data", []):   # newest first
                period, value = point.get("period", ""), point.get("value", "")
                if not period.startswith("M") or period == "M13":
                    continue   # M13 is the annual average
                try:
                    latest[series["seriesID"]] = (int(point["year"]), int(period[1:]), float(value))
                except ValueError:
                    continue   # "-" means not available that month
                break

    BLS_CACHE.mkdir(parents=True, exist_ok=True)
    cache_file = BLS_CACHE / f"{date.today().isoformat()}.json"
    cache_file.write_text(json.dumps(raw, indent=2), encoding="utf-8")
    return latest, messages


def month_end(year, month):
    """The instant a month ends (midnight UTC on the 1st of the next month), as ISO text."""
    next_year, next_month = (year + 1, 1) if month == 12 else (year, month + 1)
    return datetime(next_year, next_month, 1, tzinfo=timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def bls_prices():
    rows = read_bls_series()
    series_ids = sorted({r["series_id"].strip() for r in rows})
    print(f"Fetching {len(series_ids)} BLS series...")
    latest, messages = fetch_bls(series_ids)

    for message in messages:
        print(f"  BLS: {message}")

    prices, missing = [], []
    for row in rows:
        series_id = row["series_id"].strip()
        if series_id not in latest:
            missing.append(f"{row['ingredient']} ({series_id})")
            continue
        year, month, value = latest[series_id]
        prices.append({
            "ingredient": row["ingredient"].strip(),
            "source": "BLS",
            "price": round(value, 2),
            "size": row["size"].strip(),
            "externalId": series_id,
            "recordedAt": month_end(year, month),
            "storeName": BLS_STORE_NAME,
        })
        print(f"  {row['ingredient']:<20} {year}-{month:02d}  ${value:.2f} per {row['size'].strip()}")

    if missing:
        print(f"\nNo recent data for {len(missing)} series (discontinued or a wrong id):")
        for m in missing:
            print(f"  {m}")
    return prices


# ---------- seed CSV ----------

def seed_prices():
    with open(SEED_PRICES_PATH, newline="", encoding="utf-8") as f:
        lines = [line for line in f if line.strip() and not line.lstrip().startswith("#")]
    rows = [r for r in csv.DictReader(lines) if (r.get("ingredient") or "").strip()]
    if not rows:
        print("prices_seed.csv has no prices yet.")
        return []
    check_names(rows, SEED_PRICES_PATH)

    prices = []
    for row in rows:
        try:
            price = round(float(row["price"]), 2)
        except ValueError:
            sys.exit(f"prices_seed.csv: '{row['price']}' isn't a price ({row['ingredient']})")
        prices.append({
            "ingredient": row["ingredient"].strip(),
            "source": "SEED",
            "price": price,
            "size": row["size"].strip(),
        })
    return prices


# ---------- posting ----------

def post_prices(prices, dry_run):
    if not prices:
        return
    if dry_run:
        print(f"\nDry run: would post {len(prices)} prices. First one:")
        print(json.dumps(prices[0], indent=2))
        return

    imported = skipped = recosted = 0
    failed = []
    for start in range(0, len(prices), IMPORT_BATCH_SIZE):
        result = post_json("/api/admin/prices/import", {"prices": prices[start:start + IMPORT_BATCH_SIZE]})
        imported += result["imported"]
        skipped += result["skipped"]
        recosted = max(recosted, result["recipesRecosted"])   # each batch recalculates every recipe
        failed.extend(result["failed"])

    print(f"\nImported {imported}, skipped {skipped} already stored, "
          f"recalculated cost for {recosted} recipes.")
    if failed:
        print(f"{len(failed)} failed:")
        for f in failed:
            print(f"  {f['ingredient']}: {f['reason']}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("source", choices=["bls", "seed"])
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    prices = bls_prices() if args.source == "bls" else seed_prices()
    post_prices(prices, args.dry_run)


if __name__ == "__main__":
    main()