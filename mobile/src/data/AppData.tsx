// Shared state for the signed-in app: preferences, this week's plan, its
// grocery list, plan history and favorites. Screens read from here so a change
// on one tab (lock, swap, favorite, check off an item) shows everywhere.
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { ApiError } from '@/api/client';
import { mealPlansApi, type GroceryList, type MealPlan, type MealPlanSummary } from '@/api/mealPlans';
import { preferencesApi, type DietaryLabel, type PreferencesRequest, type PreferencesResponse } from '@/api/preferences';
import { recipesApi, type RecipeSummary } from '@/api/recipes';
import { useAuth } from '@/auth/AuthProvider';

type Loadable<T> = { data: T; loading: boolean; error: string | null };

type AppData = {
  prefs: PreferencesResponse | null;
  labels: DietaryLabel[];
  /** undefined while loading the first time; null when there's no plan this week. */
  plan: MealPlan | null | undefined;
  planError: string | null;
  planBusy: boolean;
  grocery: Loadable<GroceryList | null>;
  history: Loadable<MealPlanSummary[]>;

  refresh: () => Promise<void>;
  savePreferences: (body: PreferencesRequest) => Promise<void>;
  generate: () => Promise<void>;
  regenerate: () => Promise<void>;
  toggleLock: (mealId: string) => Promise<void>;
  swap: (mealId: string, recipeId: string) => Promise<void>;
  setPurchased: (itemId: string, purchased: boolean) => Promise<void>;
  getPlan: (id: string) => Promise<MealPlan>;

  isFavorite: (r: Pick<RecipeSummary, 'id' | 'favorite'>) => boolean;
  toggleFavorite: (r: Pick<RecipeSummary, 'id' | 'favorite'>) => Promise<void>;
};

const Ctx = createContext<AppData | null>(null);
const message = (e: unknown) => (e instanceof ApiError ? e.message : 'Something went wrong. Please try again.');

export function AppDataProvider({ children }: { children: ReactNode }) {
  const { authedRequest } = useAuth();
  const [prefs, setPrefs] = useState<PreferencesResponse | null>(null);
  const [labels, setLabels] = useState<DietaryLabel[]>([]);
  const [plan, setPlan] = useState<MealPlan | null | undefined>(undefined);
  const [planError, setPlanError] = useState<string | null>(null);
  const [planBusy, setPlanBusy] = useState(false);
  const [grocery, setGrocery] = useState<Loadable<GroceryList | null>>({ data: null, loading: false, error: null });
  const [history, setHistory] = useState<Loadable<MealPlanSummary[]>>({ data: [], loading: false, error: null });
  const [favOverrides, setFavOverrides] = useState<Record<string, boolean>>({});
  const planCache = useRef(new Map<string, MealPlan>());

  const loadGrocery = useCallback(
    async (p: MealPlan | null) => {
      if (!p) return setGrocery({ data: null, loading: false, error: null });
      setGrocery((g) => ({ ...g, loading: true, error: null }));
      try {
        setGrocery({ data: await mealPlansApi.groceryList(authedRequest, p.id), loading: false, error: null });
      } catch (e) {
        setGrocery((g) => ({ ...g, loading: false, error: message(e) }));
      }
    },
    [authedRequest],
  );

  const loadHistory = useCallback(async () => {
    setHistory((h) => ({ ...h, loading: true, error: null }));
    try {
      setHistory({ data: await mealPlansApi.list(authedRequest), loading: false, error: null });
    } catch (e) {
      setHistory((h) => ({ ...h, loading: false, error: message(e) }));
    }
  }, [authedRequest]);

  /** Apply a new current plan everywhere and refresh what depends on it. */
  const applyPlan = useCallback(
    (p: MealPlan | null, opts: { groceries?: boolean } = {}) => {
      setPlan(p);
      if (p) planCache.current.set(p.id, p);
      if (opts.groceries !== false) loadGrocery(p);
      loadHistory();
    },
    [loadGrocery, loadHistory],
  );

  const refresh = useCallback(async () => {
    setPlanError(null);
    const [p, l] = await Promise.allSettled([preferencesApi.get(authedRequest), preferencesApi.dietaryLabels()]);
    if (p.status === 'fulfilled') setPrefs(p.value);
    if (l.status === 'fulfilled') setLabels(l.value);
    try {
      applyPlan(await mealPlansApi.current(authedRequest));
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) applyPlan(null);
      else {
        setPlan((cur) => (cur === undefined ? null : cur));
        setPlanError(message(e));
      }
    }
  }, [authedRequest, applyPlan]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const run = useCallback(async (fn: () => Promise<MealPlan>, opts?: { groceries?: boolean }) => {
    setPlanBusy(true);
    setPlanError(null);
    try {
      applyPlan(await fn(), opts);
    } catch (e) {
      setPlanError(message(e));
      throw e;
    } finally {
      setPlanBusy(false);
    }
  }, [applyPlan]);

  const value = useMemo<AppData>(() => {
    const isFavorite = (r: Pick<RecipeSummary, 'id' | 'favorite'>) => favOverrides[r.id] ?? r.favorite;
    return {
      prefs,
      labels,
      plan,
      planError,
      planBusy,
      grocery,
      history,
      refresh,

      savePreferences: async (body) => {
        setPrefs(await preferencesApi.replace(authedRequest, body));
      },
      generate: () => run(() => mealPlansApi.generate(authedRequest)),
      regenerate: async () => {
        if (plan) await run(() => mealPlansApi.regenerate(authedRequest, plan.id));
      },
      toggleLock: async (mealId) => {
        if (!plan) return;
        const meal = plan.meals.find((m) => m.id === mealId);
        if (!meal) return;
        // Optimistic: flip the whole run of leftovers that share this recipe.
        const flip = (p: MealPlan): MealPlan => ({
          ...p,
          meals: p.meals.map((m) => (m.slot === meal.slot && m.recipe.id === meal.recipe.id ? { ...m, locked: !meal.locked } : m)),
        });
        setPlan(flip(plan));
        try {
          const updated = await mealPlansApi.updateMeal(authedRequest, plan.id, mealId, { locked: !meal.locked });
          setPlan(updated);
          planCache.current.set(updated.id, updated);
        } catch (e) {
          setPlan(plan);
          setPlanError(message(e));
        }
      },
      swap: async (mealId, recipeId) => {
        if (plan) await run(() => mealPlansApi.updateMeal(authedRequest, plan.id, mealId, { recipeId }));
      },
      setPurchased: async (itemId, purchased) => {
        const prev = grocery.data;
        if (!prev) return;
        setGrocery((g) => ({ ...g, data: g.data && { ...g.data, items: g.data.items.map((i) => (i.id === itemId ? { ...i, purchased } : i)) } }));
        try {
          await mealPlansApi.setPurchased(authedRequest, itemId, purchased);
        } catch (e) {
          setGrocery((g) => ({ ...g, data: prev, error: message(e) }));
        }
      },
      getPlan: async (id) => {
        const hit = planCache.current.get(id);
        if (hit) return hit;
        const p = await mealPlansApi.get(authedRequest, id);
        planCache.current.set(id, p);
        return p;
      },

      isFavorite,
      toggleFavorite: async (r) => {
        const next = !isFavorite(r);
        setFavOverrides((o) => ({ ...o, [r.id]: next }));
        try {
          await (next ? recipesApi.favorite(authedRequest, r.id) : recipesApi.unfavorite(authedRequest, r.id));
        } catch (e) {
          setFavOverrides((o) => ({ ...o, [r.id]: !next }));
          setPlanError(message(e));
        }
      },
    };
  }, [prefs, labels, plan, planError, planBusy, grocery, history, refresh, authedRequest, run, favOverrides]);

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useAppData() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useAppData must be used inside <AppDataProvider>');
  return ctx;
}
