# Forkast Mobile

The Forkast app for iOS and Android, built with React Native. This is the user-facing product: preferences, weekly meal plans, recipes, and grocery lists.

> **Status:** planned. Nothing in this folder has been built yet.

## Responsibilities

The app talks only to the Forkast backend API (see [../backend/README.md](../backend/README.md)). It never connects to the database or any third-party data source directly. All recipe parsing, nutrition, pricing, and meal plan generation happen on the backend.

## Planned Features

| Feature                                                                                    | Backend routes                                                      |
| ------------------------------------------------------------------------------------------ | ------------------------------------------------------------------- |
| Sign up, log in, email verification, password reset                                        | `/api/auth/*`                                                       |
| Onboarding: weekly budget, household size, dietary restrictions, calorie and macro targets | `/api/users/me/preferences`                                         |
| Browse and search recipes, scale servings                                                  | `/api/recipes`, `/api/recipes/{id}/scaled`                          |
| Favorite recipes                                                                           | `/api/recipes/{id}/favorite`                                        |
| Generate and view weekly meal plans (calendar view)                                        | `/api/meal-plans`, `/api/meal-plans/{id}/calendar`                  |
| Grocery list with purchased checkboxes                                                     | `/api/meal-plans/{id}/grocery-list`, `/api/grocery-list-items/{id}` |

## Auth Notes

The backend issues short-lived JWT access tokens plus refresh tokens. Tokens should be kept in secure device storage, not plain async storage.

## Tech Stack

| Area         | Technology                               |
| ------------ | ---------------------------------------- |
| Framework    | React Native                             |
| Tooling      | To be decided (Expo or React Native CLI) |
| Language     | To be decided                            |
| Distribution | App Store and Google Play                |

## Setup

To be added when the app is created (Node version, install and run commands, backend URL configuration, build and release steps).
