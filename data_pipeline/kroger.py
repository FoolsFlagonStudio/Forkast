"""Kroger product API: app token and product search, for find_product.py.

Settings come from data_pipeline/.env:
    KROGER_CLIENT_ID, KROGER_CLIENT_SECRET   required
    KROGER_BASE_URL     default https://api-ce.kroger.com (certification); use
                        https://api.kroger.com once the app is approved for production
    KROGER_LOCATION_ID  default 01400513 (Kroger On the Rhine, Cincinnati), the store
                        the backend prices from, so search results show its prices

The token and the secret are never printed.
"""
import os
import time

import requests

from common import require_env

BASE_URL = os.getenv(
    "KROGER_BASE_URL", "https://api-ce.kroger.com").rstrip("/")
LOCATION_ID = os.getenv("KROGER_LOCATION_ID", "01400513")

_token = None
_token_expires = 0.0


def token():
    """An app token (client credentials), reused until a minute before it expires."""
    global _token, _token_expires
    if _token is None or time.time() > _token_expires:
        resp = requests.post(
            f"{BASE_URL}/v1/connect/oauth2/token",
            auth=(require_env("KROGER_CLIENT_ID"),
                  require_env("KROGER_CLIENT_SECRET")),
            data={"grant_type": "client_credentials",
                  "scope": "product.compact"},
            timeout=30,
        )
        if resp.status_code != 200:
            raise SystemExit(
                f"Kroger token request failed: HTTP {resp.status_code} {resp.text[:200]}")
        body = resp.json()
        _token = body["access_token"]
        _token_expires = time.time() + max(60, body.get("expires_in", 1800) - 60)
    return _token


def search(term, limit=10):
    """Products matching term at the configured store, in Kroger's order."""
    resp = requests.get(
        f"{BASE_URL}/v1/products",
        params={"filter.term": term,
                "filter.locationId": LOCATION_ID, "filter.limit": limit},
        headers={"Authorization": f"Bearer {token()}"},
        timeout=30,
    )
    if resp.status_code != 200:
        raise RuntimeError(f"HTTP {resp.status_code} {resp.text[:200]}")
    return resp.json().get("data", [])


def clean_label(description):
    """Kroger sends "®" as mangled bytes ("Simple TruthÂ®"); drop the marks."""
    text = (description or "").replace(
        "Â", "").replace("®", "").replace("™", "")
    return " ".join(text.split())


def first_priced_item(product):
    """The first item with a regular price above zero, or None."""
    for item in product.get("items") or []:
        regular = (item.get("price") or {}).get("regular") or 0
        if regular > 0:
            return item
    return None
