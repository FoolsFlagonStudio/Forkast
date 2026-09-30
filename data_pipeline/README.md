# Forkast Ingestion

A Python script that scrapes recipe pages with [`recipe-scrapers`](https://github.com/hhursev/recipe-scrapers) and sends the raw data to the Forkast backend.

> **Status:** planned. Nothing in this folder has been built yet.

## Responsibilities

This script only **collects and forwards** raw recipe data. It does not parse ingredient lines, match ingredients, compute nutrition, or assign dietary labels. The backend owns all of that (see [../backend/README.md](../backend/README.md#recipe-ingestion-server-side-planned)).

Keeping the script thin means parsing rules live in one place, and re-running ingestion after a backend improvement doesn't require script changes.

## Flow

1. Read a list of recipe URLs.
2. Scrape each URL with `recipe-scrapers`.
3. Send recipes to the backend in batches of 25 to 50.
4. Log the per-batch response so skipped, failed, and needs-review counts are visible.

## Backend Contract

**Endpoint:** `POST /api/admin/recipes/ingest`
**Auth:** API key (header name to be decided when the endpoint is built)

### Request

An array of raw recipes:

```json
[
  {
    "sourceUrl": "https://...",
    "name": "...",
    "prepTimeMinutes": 15,
    "cookTimeMinutes": 30,
    "baseServings": 4,
    "rawIngredients": ["5 tbsp honey (sourwood is nice)"],
    "rawInstructions": ["Preheat oven to 350°F."],
    "imageUrl": "https://..."
  }
]
```

| Field             | Notes                                                                                      |
| ----------------- | ------------------------------------------------------------------------------------------ |
| `sourceUrl`       | Required. The backend uses it to skip duplicates, so re-running a URL list is safe.        |
| `rawIngredients`  | Unparsed ingredient lines, exactly as scraped.                                             |
| `rawInstructions` | One string per step, in order.                                                             |
| Nutrition         | **Do not send.** The backend computes nutrition from matched FoodData Central ingredients. |

### Response

```json
{
  "created": 42,
  "skippedDuplicate": 8,
  "failed": [{ "sourceUrl": "...", "reason": "..." }],
  "needsReview": 15
}
```

`needsReview` counts ingredient lines the backend couldn't match confidently. Those are resolved later through the admin review endpoints.

## Setup

To be added when the script is written (Python version, dependencies, configuration for the backend URL and API key).
