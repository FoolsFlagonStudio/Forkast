# Forkast

A mobile meal planner that builds your weekly recipes and grocery list around your budget, macros, and dietary needs. Spring Boot backend, React Native frontend.

Users set a weekly budget, household size, dietary restrictions, and optional calorie/macro targets. Each week Forkast generates a meal plan and an aggregated grocery list priced to fit that budget.

> **Status:** early development. The backend data model, repositories, and Flyway migrations are complete; services, auth, and API endpoints are next. The data pipeline, frontend, and mobile app have not been started.

## How It Fits Together

```
data_pipeline/  (Python)     backend/  (Spring Boot)          mobile/  (React Native)
recipe-scrapers  ── POST ──▶  parse, match, classify   ◀── REST ──  meal plans, grocery
                              recipes; generate plans              lists, preferences
                                       │
                                       ▼
                              PostgreSQL (Supabase)
```

- **data_pipeline** scrapes recipe pages and sends the raw data to the backend. It does no parsing itself.
- **backend** owns all data: recipe parsing, ingredient matching, nutrition, pricing, dietary labels, and meal plan generation.
- **mobile** is the user-facing app and talks only to the backend API.
- **frontend** is a static marketing site with general information and links to the app stores. It doesn't call the backend.

## Repository Structure

| Folder           | Contents                                                      | Docs                                               |
| ---------------- | ------------------------------------------------------------- | -------------------------------------------------- |
| `backend/`       | Spring Boot API                                               | [backend/README.md](backend/README.md)             |
| `data_pipeline/` | Python recipe scraper (planned)                               | [data_pipeline/README.md](data_pipeline/README.md) |
| `frontend/`      | Marketing website with app info and App Store links (planned) | [frontend/README.md](frontend/README.md)           |
| `mobile/`        | React Native app (planned)                                    | [mobile/README.md](mobile/README.md)               |

## Tech Stack

| Area           | Technology                                                         |
| -------------- | ------------------------------------------------------------------ |
| Backend        | Java, Spring Boot 4.1, Spring Data JPA, Hibernate 7, Flyway, Maven |
| Database       | PostgreSQL 17 on Supabase (used as a plain database only)          |
| Mobile         | React Native (planned)                                             |
| Website        | Vite + TypeScript (planned)                                        |
| Data pipeline  | Python, `recipe-scrapers` (planned)                                |
| Nutrition data | USDA FoodData Central                                              |
| Price data     | Open Prices (Open Food Facts), with manual entries as fallback     |

## Roadmap

- [x] Repository and Spring Boot setup
- [x] Database connection (Supabase)
- [x] Data model (16 tables)
- [x] Flyway migrations
- [ ] Auth (signup, login, JWT + refresh tokens)
- [ ] User and preferences endpoints
- [ ] Recipe data pipeline
- [ ] Meal plan generator
- [ ] Grocery list and pricing
- [ ] Mobile app
- [ ] Marketing website
