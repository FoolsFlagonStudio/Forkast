"""Print the FDC description for one or more ids: python fdc_lookup.py 174036 173414"""
import sys

import requests

from common import require_env

ids = [int(arg) for arg in sys.argv[1:]]
if not ids:
    sys.exit("usage: python fdc_lookup.py <fdc_id> [<fdc_id> ...]")

resp = requests.get(
    "https://api.nal.usda.gov/fdc/v1/foods",
    params={"api_key": require_env("DATA_GOV_API_KEY"), "fdcIds": ",".join(
        map(str, ids)), "format": "abridged"},
    timeout=30,
)
if not resp.ok:
    # never print resp.url, it has the key
    sys.exit(f"FDC returned {resp.status_code}")

found = {food["fdcId"]: food for food in resp.json()}
for fdc_id in ids:
    food = found.get(fdc_id)
    print(f"{fdc_id}  " +
          (f"{food['dataType']:<12} {food['description']}" if food else "NOT FOUND"))
