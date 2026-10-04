# Forkast Data Pipeline

Python scripts that build Forkast's ingredient catalog from USDA FoodData Central and scrape recipe pages with [`recipe-scrapers`](https://github.com/hhursev/recipe-scrapers), then send everything to the backend's admin API.

> **Status:** working. The catalog has 153 ingredients, and the first scrape imported 17 recipes from 2 sites.

## Responsibilities

The scripts only **fetch and forward**. They never parse ingredient lines, match ingredients, compute nutrition, or assign dietary labels; the backend owns all of that (see [../backend/README.md](../backend/README.md#recipe-ingestion)).

Keeping the scripts thin means parsing rules live in one place, and the backend can re-process saved recipes after a fix without re-scraping.

## Files

| File                         | Purpose                                                                                                               |
| ---------------------------- | --------------------------------------------------------------------------------------------------------------------- | ------------ |
| `common.py`                  | Loads `.env`, the backend URL, cache and log folders, the User-Agent, and `post_json` (adds the `X-Admin-Key` header) |
| `ingredients_seed.csv`       | The curated ingredient catalog: `name, query, fdc_id, fdc_description, aliases, tags` (aliases and tags are `         | `-separated) |
| `find_fdc.py`                | Fills in blank `fdc_id`s by searching FDC; writes alternatives to `cache/fdc_candidates.csv`                          |
| `fdc_lookup.py`              | Prints the FDC description for one or more ids, to check a hand-picked id                                             |
| `seed_ingredients.py`        | Fetches each food from FDC, extracts macros and portions, posts to the import route                                   |
| `urls.txt`                   | Recipe URLs, one per line; `#` lines are comments                                                                     |
| `scrape_recipes.py`          | Fetches, caches, scrapes and posts recipes; writes a JSON log to `logs/`                                              |
| `samples/ingest_sample.json` | A hand-written batch for testing the ingest route                                                                     |
| `cache/`, `logs/`            | Cached FDC responses and HTML, run logs; both gitignored                                                              |

## Setup

From `data_pipeline/` in Git Bash:

```bash
python -m venv .venv
source .venv/Scripts/activate      # run this in every new terminal
pip install -r requirements.txt
cp .env.example .env               # then fill in the values
```

| `.env` setting          | Value                                                                                     |
| ----------------------- | ----------------------------------------------------------------------------------------- |
| `FORKAST_API_URL`       | `http://localhost:8080`                                                                   |
| `FORKAST_ADMIN_KEY`     | `openssl rand -hex 32`; the same value goes in the backend's `forkast.admin.api-key`      |
| `DATA_GOV_API_KEY`      | A free key from [api.data.gov](https://api.data.gov/signup) (1,000 FDC requests per hour) |
| `SCRAPER_CONTACT_EMAIL` | An address site owners can reach you at; sent in every request's User-Agent               |

`.env` is gitignored. `.env.example` is committed with blank values; never put real keys in it.

## Ingredient catalog

```bash
python find_fdc.py            # fill blank fdc_ids, then review the matches
python fdc_lookup.py 174036   # check a hand-picked id
python seed_ingredients.py --dry-run
python seed_ingredients.py    # 0 created / N updated on a re-run
```

**Picking FDC foods.** `find_fdc.py` searches SR Legacy first, because its household portions ("1 large", "1 cup, chopped", "1 clove") are what recipe conversion needs; Foundation is the fallback. FDC search ranking is unreliable, so read every `fdc_description`. Watch for restaurant brands, cooked versions, branded items, and the wrong animal ("ground beef" matched ground turkey). Fix a row by pasting an id from `cache/fdc_candidates.csv`, or by clearing `fdc_id`, editing `query`, and re-running. A word starting with `-` in a query excludes that word, and `/` makes FDC return 400.

**Checks before any API call.** Names must be lowercase and unique, aliases can't repeat across rows, tags must be valid, and two rows can't share an `fdc_id` (the backend updates by it).

**Extraction** (`seed_ingredients.py`):

| Value               | From                                                                                                                                                                                                                                      |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Calories            | Nutrient 208; 958, then 957 (Atwater) for Foundation foods that lack 208                                                                                                                                                                  |
| Protein, carbs, fat | Nutrients 203, 205, 204                                                                                                                                                                                                                   |
| Portions            | `foodPortions`: Foundation keeps the unit in `measureUnit.name`; SR Legacy sets it to "undetermined" and puts it in `modifier`. Stored as the weight of ONE unit (0.5 cup = 44 g becomes cup = 88 g). RACC and NLEA servings are skipped. |

All values are per 100 g. Negative values (carbs "by difference" can come out slightly below zero) are clamped to 0.

**Tags** drive dietary labels: MEAT, POULTRY, FISH, SHELLFISH, DAIRY, EGG, GLUTEN, TREE_NUT, PEANUT, SOY, HONEY, GELATIN. Broths carry POULTRY or MEAT; Worcestershire carries FISH (anchovies).

## Scraping recipes

```bash
python scrape_recipes.py --dry-run   # fetch and cache, print one payload, post nothing
python scrape_recipes.py             # post (reads the cache, so the sites aren't hit again)
```

### Rules the script follows

- Checks each site's `robots.txt` and skips disallowed paths. A missing robots.txt allows everything; 401/403 or an unreachable one means the site is skipped.
- One request at a time, 3 to 5 seconds apart per site, or the site's `Crawl-delay` if longer.
- Identifies itself with `ForkastRecipeBot/0.1 (personal project; contact <email>)`.
- Caches every page in `cache/html/`, so re-runs never download a page twice.
- Never works around logins, paywalls or bot protection. Damn Delicious returns 403 to automated requests, so it is not used.
- Read a site's terms of use before adding it to `urls.txt`.

**Sites in use:** sipandfeast.com and budgetbytes.com (both supported by `recipe-scrapers`). Unsupported sites fall back to schema.org data (`supported_only=False`).

### Copyright

Ingredient lists are facts and generally not protected; instructions, descriptions and photos usually are. Storing them for local development and a portfolio demo is fine. Before a public release, choose one: show ingredients, nutrition and cost with a link to the source for instructions; use a licensed recipe API; use recipes you have rights to; or ask sources for permission. Keep `source_url` and show attribution either way. This is not legal advice.

## Backend contract

All routes require the `X-Admin-Key` header.

### `POST /api/admin/ingredients/import` (up to 50)

```json
{
  "ingredients": [
    {
      "fdcId": 171077,
      "name": "chicken breast",
      "aliases": ["chicken breasts"],
      "tags": ["POULTRY"],
      "caloriesPer100g": 120.0,
      "proteinGPer100g": 22.5,
      "carbsGPer100g": 0.0,
      "fatGPer100g": 2.62,
      "portions": [{ "description": "piece", "gramWeight": 272.0 }]
    }
  ]
}
```

Response: `{ "created": 50, "updated": 0 }`. Matching on `fdcId` makes re-runs safe.

### `POST /api/admin/recipes/ingest` (up to 25)

```json
{
  "recipes": [
    {
      "sourceUrl": "https://www.sipandfeast.com/chicken-riggies/",
      "host": "sipandfeast.com",
      "name": "Chicken Riggies",
      "description": "...",
      "prepTimeMinutes": 5,
      "cookTimeMinutes": 40,
      "yields": "6 servings",
      "rawIngredients": [
        "1 pound (454g) rigatoni",
        "8 cloves garlic (chopped)"
      ],
      "rawInstructions": ["Bring a large pot of salted water to boil.", "..."],
      "imageUrl": "https://...",
      "category": "Main Course",
      "cuisine": "Italian",
      "keywords": ["chicken riggies"]
    }
  ]
}
```

Response: `{ "created": 17, "skippedDuplicate": 0, "failed": [{ "sourceUrl": "...", "reason": "..." }], "needsReview": 41 }`. A bad recipe is listed in `failed` without affecting the rest. `yields` is sent raw and parsed by the backend; missing fields are sent as `null`. Nutrition is never sent: the backend computes it.

### Review and re-processing

| Route                                                           | Use                                                                                                                                             |
| --------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| `GET /api/admin/recipe-ingredients/needs-review?page=0&size=50` | Lines that didn't match confidently, grouped by parsed name                                                                                     |
| `PATCH /api/admin/recipe-ingredients/{id}`                      | `{ "ingredientId": "...", "saveAlias": true }` matches the line, saves its name as an alias, and matches every other queued line with that name |
| `POST /api/admin/recipes/reprocess`                             | Re-parses saved recipes after a parser or catalog change (`?rematchAll=true` also re-matches already-matched lines)                             |

```bash
KEY=$(grep FORKAST_ADMIN_KEY .env | cut -d= -f2 | tr -d '\r')
curl -s "http://localhost:8080/api/admin/recipe-ingredients/needs-review?size=100" -H "X-Admin-Key: $KEY" | python -m json.tool
curl -s -X POST "http://localhost:8080/api/admin/recipes/reprocess" -H "X-Admin-Key: $KEY"
```

## Improving the catalog over time

1. Scrape more recipes.
2. Find what didn't match, most common first:
   ```sql
   select ri.parsed_name, count(*) as times, max(ri.match_score) as best_score, min(ri.raw_text) as example
   from recipe_ingredients ri
   where ri.needs_review
   group by ri.parsed_name
   order by times desc;
   ```
3. Fix the cause: a parser pattern (backend), a new alias or row in `ingredients_seed.csv`, or a manual match in the review queue.
4. Re-run `find_fdc.py` and `seed_ingredients.py` for catalog changes, then `POST /api/admin/recipes/reprocess`.

Planned for the next catalog pass: cooked rice, pasta and grains ("cooked white rice" currently matches raw rice, inflating calories); white wine, pecorino, capers, olives, pine nuts, molasses and canned green chiles; aliases for pasta shapes (paccheri, pappardelle, pastina). "cooking oil" was resolved through the review queue and is already an alias of vegetable oil. Find cooked lines with `select ri.raw_text, i.name from recipe_ingredients ri join ingredients i on i.id = ri.ingredient_id where ri.raw_text ilike '%cooked%';`.
