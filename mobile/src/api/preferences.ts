// Mirrors backend user/PreferencesController and diet/DietaryLabelController.
import { request } from './client';

export type MealSlot = 'BREAKFAST' | 'LUNCH' | 'DINNER';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export type DietaryLabel = { id: string; name: string };

/** PreferencesRequest. Macro targets are per meal (see RecipeFitEvaluator). */
export type PreferencesRequest = {
  weeklyBudget: number;
  householdServingSize: number;
  maxCaloriesPerMeal?: number | null;
  maxCaloriesPerDay?: number | null;
  proteinTargetGrams?: number | null;
  carbsTargetGrams?: number | null;
  fatTargetGrams?: number | null;
  repeatAvoidanceDays: number;
  planStartDay: DayOfWeek;
  prefersMealPrep?: boolean | null;
  dietaryLabelIds: string[];
  /** Recipes per week for each included slot (1-7). */
  mealSlots: Partial<Record<MealSlot, number>>;
};

export type PreferencesResponse = Omit<PreferencesRequest, 'dietaryLabelIds'> & {
  dietaryRestrictions: DietaryLabel[];
  updatedAt: string;
};

type Authed = <T>(path: string, opts?: { method?: 'GET' | 'POST' | 'PUT'; body?: unknown }) => Promise<T>;

export const preferencesApi = {
  dietaryLabels: () => request<DietaryLabel[]>('/api/dietary-labels'),
  get: (authed: Authed) => authed<PreferencesResponse>('/api/users/me/preferences'),
  create: (authed: Authed, body: PreferencesRequest) =>
    authed<PreferencesResponse>('/api/users/me/preferences', { method: 'POST', body }),
  replace: (authed: Authed, body: PreferencesRequest) =>
    authed<PreferencesResponse>('/api/users/me/preferences', { method: 'PUT', body }),
};
