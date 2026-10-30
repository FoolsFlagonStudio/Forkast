# Meal Plan API: what the app expects

The app is built against these routes. Until the backend has them, `features.mockMealPlans` (on by default) serves them on the device from `src/api/mock`, planning with your real recipes from `/api/recipes`. Once the backend ships them, set `EXPO_PUBLIC_MOCK_PLANS=0` and nothing else in the app changes.

Types live in `src/api/mealPlans.ts`. The planned routes in `backend/README.md` are kept; additions are marked **new**.

## Routes

| Method | Route | Returns | Notes |
| --- | --- | --- | --- |
| GET | `/api/meal-plans/current` **new** | `MealPlan` | Plan whose week contains today (by the user's `planStartDay`). 404 when there's none. |
| GET | `/api/meal-plans` | `MealPlanSummary[]` | Newest first. Feeds Plan > History and the Budget chart. |
| GET | `/api/meal-plans/{id}` | `MealPlan` | |
| POST | `/api/meal-plans/generate` | `MealPlan` | Body `{ weekStartDate? }`, defaults to the current week. Replaces an existing plan for that week. |
| POST | `/api/meal-plans/{id}/regenerate` **new** | `MealPlan` | New recipes for every meal that isn't locked. Rebuilds the grocery list. |
| PATCH | `/api/meal-plans/{id}/meals/{mealId}` **new** | `MealPlan` | Body `{ locked?, recipeId? }`. Lock or swap. Applies to the whole run of days the recipe covers (see below). |
| GET | `/api/meal-plans/{id}/meals/{mealId}/alternatives?limit=` **new** | `RecipeSummary[]` | Swap suggestions for that slot that fit the user's restrictions and aren't already in the plan, cheapest first. |
| DELETE | `/api/meal-plans/{id}` | 204 | |
| GET | `/api/meal-plans/{id}/grocery-list` | `GroceryList` | |
| PATCH | `/api/grocery-list-items/{id}` | `GroceryListItem` | Body `{ purchased }`. |

## Shapes

```ts
MealPlan = {
  id, weekStartDate, weekEndDate,        // local dates, YYYY-MM-DD
  budgetTarget, estimatedTotalCost, costComplete,
  costBySlot: { BREAKFAST?, LUNCH?, DINNER? },   // Budget page breakdown
  meals: PlannedMeal[],                  // 7 days x included slots, sorted by date then slot
  generatedAt,
}
PlannedMeal = {
  id, date, dayOfWeek, slot,
  recipe: RecipeSummary,                 // same shape as /api/recipes items
  servings,                              // household size
  leftover: boolean,                     // repeat of a recipe cooked earlier this week
  locked: boolean,
}
GroceryList = { mealPlanId, estimatedTotalCost, items: GroceryListItem[] }
GroceryListItem = { id, name, aisle, totalAmount, displayAmount, unit, estimatedCost, purchased }
Aisle = PRODUCE | MEAT | SEAFOOD | DAIRY | EGGS | BAKERY | FROZEN | PANTRY | OTHER
```

## Behavior the UI relies on

- **Runs and leftovers.** A slot with N recipes per week splits the 7 days into N runs (3 -> 3, 2, 2 days). The first day of a run is cooked, the rest are `leftover: true`. Locking or swapping any day changes the whole run. This is what the `plan_selections` idea in the backend roadmap would store.
- **Grocery amounts** cover every serving in a run (household x days), skip optional lines, and round whole-unit items (count, can, clove) up.
- **Aisles.** Ingredients have no aisle yet. The grocery list needs one per item, most simply an `aisle` column on `ingredients` (the 153-item catalog can be tagged once). The mock guesses from the name.
- **Errors** use the existing ProblemDetail format; the app shows `detail`.

## Preferences additions

`PreferencesRequest` / `PreferencesResponse` gain two fields (the app already sends them; Spring ignores unknown fields until they exist):

| Field | Type | Meaning |
| --- | --- | --- |
| `autoPlanEnabled` | boolean, default true | Build next week's plan automatically |
| `autoPlanDay` | DayOfWeek, default the day before `planStartDay` | Which day to build it |

The scheduled generator would run daily and, for each user whose `autoPlanDay` is today, generate the plan for the next week starting on their `planStartDay`. Until the backend returns these fields the app keeps a local copy (`preferencesApi` in `src/api/preferences.ts`).

## Not on the backend yet (kept on the device)

- **Ratings** from the cook mode finish screen (`victual.ratings` in AsyncStorage). A `PUT /api/recipes/{id}/rating { value: UP | DOWN }` would let the generator favor liked recipes.
