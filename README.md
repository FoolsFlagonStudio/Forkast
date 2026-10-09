# Forkast

A mobile meal planner that builds your weekly recipes and grocery list around your budget, macros, and dietary needs. Spring Boot backend, React Native frontend.

Users set a weekly budget, household size, dietary restrictions, and optional calorie/macro targets. Each week Forkast generates a meal plan and an aggregated grocery list priced to fit that budget.

> **Status:** early development. The backend data model, Flyway migrations, authentication, user preferences, and the recipe data pipeline are complete: a 153-ingredient catalog from USDA FoodData Central, and scraped recipes with parsed ingredients, nutrition, dietary labels and meal-prep scores. Recipe search, scaled detail and favorites are live, and every recipe has an estimated cost per serving from weekly Kroger prices. The meal plan generator is next. The frontend and mobile app have not been started.

## How It Fits Together

```
data_pipeline/  (Python)     backend/  (Spring Boot)          mobile/  (React Native)
recipe-scrapers  ── POST ──▶  parse, match, classify   ◀── REST ──  meal plans, grocery
                              recipes; generate plans              lists, preferences
                                       │
                                       ▼
                              PostgreSQL (Supabase)
```

- **data_pipeline** builds the ingredient catalog from FoodData Central, scrapes recipe pages, maps ingredients to Kroger products, imports BLS average prices, and sends it all to the backend. It does no parsing or pricing math itself.
- **backend** owns all data: recipe parsing, ingredient matching, nutrition, pricing (a weekly Kroger refresh and recipe cost per serving), dietary labels, and meal plan generation.
- **mobile** is the user-facing app and talks only to the backend API.
- **frontend** is a static marketing site with general information and links to the app stores. It doesn't call the backend.

## Repository Structure

| Folder           | Contents                                                      | Docs                                               |
| ---------------- | ------------------------------------------------------------- | -------------------------------------------------- |
| `backend/`       | Spring Boot API                                               | [backend/README.md](backend/README.md)             |
| `data_pipeline/` | Python catalog builder and recipe scraper                     | [data_pipeline/README.md](data_pipeline/README.md) |
| `frontend/`      | Marketing website with app info and App Store links (planned) | [frontend/README.md](frontend/README.md)           |
| `mobile/`        | React Native app (planned)                                    | [mobile/README.md](mobile/README.md)               |

## Tech Stack

| Area           | Technology                                                                                         |
| -------------- | -------------------------------------------------------------------------------------------------- |
| Backend        | Java, Spring Boot 4.1, Spring Data JPA, Hibernate 7, Flyway, Maven, JUnit 5 + AssertJ              |
| Database       | PostgreSQL 17 on Supabase (used as a plain database only), `pg_trgm` for fuzzy ingredient matching |
| Mobile         | React Native (planned)                                                                             |
| Website        | Vite + TypeScript (planned)                                                                        |
| Data pipeline  | Python, `recipe-scrapers`, `requests`                                                              |
| Nutrition data | USDA FoodData Central                                                                              |
| Price data     | Kroger Products API (one store, refreshed weekly), BLS average prices, hand-entered seed prices    |

## Roadmap

- [x] Repository and Spring Boot setup
- [x] Database connection (Supabase)
- [x] Data model (16 tables)
- [x] Flyway migrations
- [x] Auth (signup, login, JWT + refresh tokens)
- [x] User and preferences endpoints
- [x] Recipe data pipeline (catalog, scraper, parser, matcher, nutrition, labels, review queue)
- [x] Recipe endpoints (search with fit flags, scaled detail, favorites)
- [x] Pricing (Kroger + BLS prices, recipe cost per serving, weekly refresh)
- [ ] Meal plan generator and grocery list
- [ ] Mobile app
- [ ] Marketing website

## Stretch Features

Ideas deliberately left out of v1:

- **Per-slot day selection.** Let a meal slot cover only some days (e.g. lunches on weekdays only). In v1 every included slot covers all 7 days of the plan.
- **Push notifications.** v1 generates each user's plan automatically before their plan start day; a push notification when it's ready comes later.
- **Regional prices.** v1 prices everything from one Kroger store; letting users pick a store or ZIP code comes later.
