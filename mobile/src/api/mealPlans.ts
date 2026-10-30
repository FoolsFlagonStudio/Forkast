// Meal plan + grocery list contract. The backend's MealPlanController and
// GroceryListController don't exist yet; until they do these routes are served
// by the in-app mock (src/api/mock). Full contract: mobile/docs/meal-plan-api.md
import type { DayOfWeek, MealSlot } from './preferences';
import type { RecipeSummary } from './recipes';
import { query } from './client';

export type PlannedMeal = {
  id: string;
  /** Local date, YYYY-MM-DD. */
  date: string;
  dayOfWeek: DayOfWeek;
  slot: MealSlot;
  recipe: RecipeSummary;
  servings: number;
  /** True when this is a repeat of a recipe cooked earlier in the week. */
  leftover: boolean;
  /** Locked meals survive regenerate. Applies to every day the recipe covers. */
  locked: boolean;
};

export type MealPlanSummary = {
  id: string;
  weekStartDate: string;
  weekEndDate: string;
  budgetTarget: number;
  estimatedTotalCost: number;
  costComplete: boolean;
};

export type MealPlan = MealPlanSummary & {
  costBySlot: Partial<Record<MealSlot, number>>;
  meals: PlannedMeal[];
  generatedAt: string;
};

export type Aisle = 'PRODUCE' | 'MEAT' | 'SEAFOOD' | 'DAIRY' | 'EGGS' | 'BAKERY' | 'FROZEN' | 'PANTRY' | 'OTHER';

export type GroceryListItem = {
  id: string;
  name: string;
  aisle: Aisle;
  totalAmount: number | null;
  displayAmount: string | null;
  unit: string | null;
  estimatedCost: number | null;
  purchased: boolean;
};

export type GroceryList = {
  mealPlanId: string;
  estimatedTotalCost: number;
  items: GroceryListItem[];
};

type Authed = <T>(path: string, opts?: { method?: 'GET' | 'POST' | 'PATCH'; body?: unknown }) => Promise<T>;

export const mealPlansApi = {
  /** 404 when there's no plan for the current week. */
  current: (authed: Authed) => authed<MealPlan>('/api/meal-plans/current'),
  list: (authed: Authed) => authed<MealPlanSummary[]>('/api/meal-plans'),
  get: (authed: Authed, id: string) => authed<MealPlan>(`/api/meal-plans/${id}`),
  generate: (authed: Authed, weekStartDate?: string) =>
    authed<MealPlan>('/api/meal-plans/generate', { method: 'POST', body: { weekStartDate } }),
  regenerate: (authed: Authed, id: string) => authed<MealPlan>(`/api/meal-plans/${id}/regenerate`, { method: 'POST' }),
  updateMeal: (authed: Authed, planId: string, mealId: string, body: { locked?: boolean; recipeId?: string }) =>
    authed<MealPlan>(`/api/meal-plans/${planId}/meals/${mealId}`, { method: 'PATCH', body }),
  alternatives: (authed: Authed, planId: string, mealId: string, limit = 8) =>
    authed<RecipeSummary[]>(`/api/meal-plans/${planId}/meals/${mealId}/alternatives${query({ limit })}`),
  groceryList: (authed: Authed, planId: string) => authed<GroceryList>(`/api/meal-plans/${planId}/grocery-list`),
  setPurchased: (authed: Authed, itemId: string, purchased: boolean) =>
    authed<GroceryListItem>(`/api/grocery-list-items/${itemId}`, { method: 'PATCH', body: { purchased } }),
};
