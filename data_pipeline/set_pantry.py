"""Marks pantry staples in ingredients_seed.csv (the pantry column).

Pantry staples go in the grocery list's "check your pantry" group and count at what the week
uses (the teaspoon, not the jar) unless the user marks one "need to buy".

Safe to run again: it only sets pantry=true for the names below and leaves every other row as
it is. Then run seed_ingredients.py to send the flags to the backend.

    python set_pantry.py            # update the CSV
    python set_pantry.py --dry-run  # show what would change
"""

import argparse
import csv
from pathlib import Path

CSV_PATH = Path(__file__).parent / "ingredients_seed.csv"

# Salt, pepper, oils, vinegars, dried spices, flour, sugar, leaveners: kept at home and bought
# rarely, so a week's plan shouldn't pay for the whole jar.
STAPLES = {
    # basics
    "salt", "black pepper",
    # oils and vinegars
    "olive oil", "vegetable oil", "sesame oil",
    "white vinegar", "apple cider vinegar", "red wine vinegar", "balsamic vinegar",
    # baking
    "all-purpose flour", "cornstarch", "granulated sugar", "brown sugar",
    "baking powder", "baking soda", "vanilla extract",
    # dried herbs and spices
    "garlic powder", "onion powder", "paprika", "chili powder", "cumin", "oregano", "thyme",
    "rosemary", "italian seasoning", "bay leaf", "cinnamon", "red pepper flakes",
    "curry powder", "turmeric", "ground ginger", "nutmeg",
}


def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dry-run", action="store_true",
                        help="show the changes without writing")
    args = parser.parse_args()

    with CSV_PATH.open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fields = list(reader.fieldnames or [])
        rows = list(reader)

    if "pantry" not in fields:
        fields.append("pantry")

    changed, found = [], set()
    for row in rows:
        name = (row.get("name") or "").strip().lower()
        if name in STAPLES:
            found.add(name)
            if (row.get("pantry") or "").strip().lower() != "true":
                row["pantry"] = "true"
                changed.append(name)

    print(f"{len(found)} staples found, {len(changed)} newly marked: {', '.join(sorted(changed)) or 'none'}")
    missing = sorted(STAPLES - found)
    if missing:
        print(f"Not in the CSV (skipped): {', '.join(missing)}")

    if args.dry_run or not changed:
        return
    with CSV_PATH.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)
    print(f"Wrote {CSV_PATH.name}. Next: python seed_ingredients.py")


if __name__ == "__main__":
    main()
