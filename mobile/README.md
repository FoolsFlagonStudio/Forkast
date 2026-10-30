# Forkast Mobile

The Forkast app for iOS and Android, built with React Native. This is the user-facing product: preferences, weekly meal plans, recipes, and grocery lists.

> **Status:** planned. Nothing in this folder has been built yet.

## Responsibilities

The app talks only to the Forkast backend API (see [../backend/README.md](../backend/README.md)). It never connects to the database or any third-party data source directly. All recipe parsing, nutrition, pricing, and meal plan generation happen on the backend.

## Planned Features

| Feature                                                                                                                                         | Backend routes                                                                                                                              |
| ----------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| Sign up, log in, email verification, password reset                                                                                             | `/api/auth/*`                                                                                                                               |
| Onboarding: weekly budget, household size, dietary restrictions, calorie and macro targets, meals per week, plan start day, leftovers for lunch | `/api/users/me/preferences`                                                                                                                 |
| Timezone, sent at signup or from the device (when "11pm the night before" is)                                                                   | `PATCH /api/users/me` (`timezone`)                                                                                                          |
| Browse and search recipes (with fit flags), scale servings                                                                                      | `/api/recipes`, `/api/recipes/{id}?servings=`                                                                                               |
| Recipe cost: per-serving estimate on cards, `sort=cost`, `maxCost` filter, total for the chosen servings in detail                              | `/api/recipes`, `/api/recipes/{id}?servings=`                                                                                               |
| Favorite recipes                                                                                                                                | `PUT`/`DELETE /api/recipes/{id}/favorite`, `/api/users/me/favorites`                                                                        |
| This week's plan (calendar from `days`, open meals from `open`), past plans, delete                                                             | `/api/meal-plans/current`, `/api/meal-plans`, `/api/meal-plans/{id}`                                                                        |
| Make a plan now (otherwise one appears at 11pm the night before the start day)                                                                  | `POST /api/meal-plans/generate`                                                                                                             |
| Lock a meal, swap it (a suggestion or "pick for me"), redraw the rest                                                                           | `PATCH .../selections/{id}`, `POST .../selections/{id}/swap`, `GET .../selections/{id}/suggestions`, `POST /api/meal-plans/{id}/regenerate` |
| Grocery list: to-buy and "check your pantry" groups, packages and costs, purchased checkboxes, "need to buy" for staples                        | `/api/meal-plans/{id}/grocery-list`, `PATCH /api/grocery-list-items/{id}`                                                                   |

## Auth Notes

The backend issues short-lived JWT access tokens plus refresh tokens. Tokens should be kept in secure device storage, not plain async storage.

## Cost Notes

`costPerServing` is an estimate in USD from one representative store, refreshed weekly. When `costComplete` is false, some lines couldn't be priced and the real cost is higher; show it as "from $X" or with an "estimate" label rather than as an exact price. `estimatedCost` in recipe detail is what the chosen servings use, not what a shopping trip costs (the grocery list buys whole packages).

## Meal Plan Notes

- A plan is a list of `selections` (one recipe cooked once, eaten over `daysCovered` days) plus a `days` calendar. Lock and swap act on a selection, so a swap changes every day that recipe covers. A meal marked `leftover` is eaten from an earlier cook.
- A selection whose cook day is today or earlier can't be swapped (409); grey out its swap button.
- Compare `groceryCost` (what the trip costs) with `budget`; `overBudget` is set from it. `usageCost` is what the meals use.
- Grocery amounts are exact; show `displayAmount`. A line with `estimate: true` has no package to price it by. Pantry lines count at what the week uses; offer "need to buy" to switch one to its full package price.
- Grocery line ids survive swaps and regenerates, and so do the purchased checkmarks.

## Tech Stack

| Area         | Technology                               |
| ------------ | ---------------------------------------- |
| Framework    | React Native                             |
| Tooling      | To be decided (Expo or React Native CLI) |
| Language     | To be decided                            |
| Distribution | App Store and Google Play                |

## Setup

To be added when the app is created (Node version, install and run commands, backend URL configuration, build and release steps).
