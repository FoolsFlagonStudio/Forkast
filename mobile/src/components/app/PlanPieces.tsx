// Plan-related pieces used on Home, Plan and Budget.
import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, View } from 'react-native';
import { router } from 'expo-router';
import Svg, { Line, Rect } from 'react-native-svg';
import { mealPlansApi, type MealPlan, type PlannedMeal } from '@/api/mealPlans';
import type { RecipeSummary } from '@/api/recipes';
import { recipesApi } from '@/api/recipes';
import { runOf } from '@/api/mock/planner';
import { useAuth } from '@/auth/AuthProvider';
import { Text } from '@/components/Text';
import { useAppData } from '@/data/AppData';
import { dayLabel, formatRange, shortDate } from '@/lib/dates';
import { macroLine, money, perServing } from '@/lib/format';
import { fontFamily } from '@/theme/fonts';
import { tokens } from '@/theme/tokens';
import { useTheme } from '@/theme/themeContext';
import { ConfirmSheet, Sheet } from './Sheet';
import { MealCard } from './MealCard';
import { ErrorNote, tap } from './ui';

/** Sage pill button (Regen / Swap / Favs). */
export function PillButton({ label, onPress, disabled, accessibilityLabel }: { label: string; onPress: () => void; disabled?: boolean; accessibilityLabel?: string }) {
  const { colors } = useTheme();
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel ?? label}
      style={({ pressed }) => [styles.pill, { backgroundColor: colors.primary, opacity: disabled ? 0.5 : pressed ? 0.85 : 1 }]}
    >
      <Text variant="option" style={{ color: colors.onAccent }}>
        {label}
      </Text>
    </Pressable>
  );
}

/** Today's (or a chosen day's) meals with lock, favorite, and open-recipe. */
export function DayMeals({ meals }: { meals: PlannedMeal[] }) {
  const { isFavorite, toggleFavorite, toggleLock } = useAppData();
  return (
    <View style={{ gap: 14 }}>
      {meals.map((m) => (
        <MealCard
          key={m.id}
          recipe={m.recipe}
          slot={m.slot}
          leftover={m.leftover}
          locked={m.locked}
          favorite={isFavorite(m.recipe)}
          onPress={() => router.push({ pathname: '/recipe/[id]', params: { id: m.recipe.id, servings: String(m.servings) } })}
          onToggleFavorite={() => toggleFavorite(m.recipe)}
          onToggleLock={() => toggleLock(m.id)}
        />
      ))}
    </View>
  );
}

/** Regen / Swap / Favs row plus the sheets they open. `meals` are the ones on screen. */
export function PlanActions({ meals, cheaperFirst }: { meals: PlannedMeal[]; cheaperFirst?: boolean }) {
  const { plan, regenerate, planBusy } = useAppData();
  const [confirm, setConfirm] = useState(false);
  const [swap, setSwap] = useState<null | 'suggestions' | 'favorites'>(null);
  if (!plan) return null;
  const locked = new Set(plan.meals.filter((m) => m.locked).map((m) => m.recipe.id)).size;

  return (
    <>
      <View style={styles.actions}>
        <PillButton label="Regen" accessibilityLabel="Regenerate this week" onPress={() => setConfirm(true)} disabled={planBusy} />
        <PillButton label="Swap" accessibilityLabel="Swap a meal" onPress={() => setSwap('suggestions')} disabled={planBusy} />
        <PillButton label="Favs" accessibilityLabel="Swap in a favorite" onPress={() => setSwap('favorites')} disabled={planBusy} />
      </View>
      {planBusy ? <ActivityIndicator style={{ marginTop: 12 }} /> : null}
      <ConfirmSheet
        visible={confirm}
        title="Regenerate this week?"
        body={
          locked
            ? `We'll pick new recipes for every meal except the ${locked} you've locked, and rebuild your grocery list.`
            : "We'll pick new recipes for the whole week and rebuild your grocery list. Lock any meal you want to keep first."
        }
        confirm="Regenerate"
        onClose={() => setConfirm(false)}
        onConfirm={() => {
          setConfirm(false);
          regenerate().catch(() => {});
        }}
      />
      <SwapSheet visible={!!swap} initialTab={swap ?? 'suggestions'} meals={meals} cheaperFirst={cheaperFirst} onClose={() => setSwap(null)} />
    </>
  );
}

type SwapProps = {
  visible: boolean;
  onClose: () => void;
  /** Meals to choose from. With one meal the picker is skipped. */
  meals: PlannedMeal[];
  initialTab: 'suggestions' | 'favorites';
  cheaperFirst?: boolean;
};

/** Pick a meal, then a replacement from suggestions or favorites. Swaps every leftover day too. */
export function SwapSheet({ visible, onClose, meals, initialTab, cheaperFirst }: SwapProps) {
  const { colors } = useTheme();
  const { authedRequest } = useAuth();
  const { plan, swap } = useAppData();
  const [meal, setMeal] = useState<PlannedMeal | null>(null);
  const [tab, setTab] = useState(initialTab);
  const [options, setOptions] = useState<RecipeSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Cooked meals only: swapping a leftover swaps the meal it came from.
  const choices = useMemo(() => {
    if (!plan) return [];
    const seen = new Set<string>();
    return meals
      .map((m) => runOf(plan, m.id)[0] ?? m)
      .filter((m) => (seen.has(m.id) ? false : (seen.add(m.id), true)))
      .sort((a, b) => (cheaperFirst ? (b.recipe.costPerServing ?? 0) - (a.recipe.costPerServing ?? 0) : 0));
  }, [plan, meals, cheaperFirst]);

  useEffect(() => {
    if (!visible) return;
    setTab(initialTab);
    setMeal(choices.length === 1 ? choices[0] : null);
  }, [visible, initialTab, choices]);

  useEffect(() => {
    if (!visible || !meal || !plan) return;
    let cancelled = false;
    setOptions(null);
    setError(null);
    const load =
      tab === 'favorites'
        ? recipesApi.favorites(authedRequest, 0, 30).then((p) => p.items)
        : mealPlansApi.alternatives(authedRequest, plan.id, meal.id, 10);
    load
      .then((list) => !cancelled && setOptions(list.filter((r) => r.id !== meal.recipe.id)))
      .catch(() => !cancelled && setError("Couldn't load options. Try again."));
    return () => {
      cancelled = true;
    };
  }, [visible, meal, tab, plan, authedRequest]);

  const run = meal && plan ? runOf(plan, meal.id) : [];
  const days = run.map((m) => dayLabel(m.dayOfWeek).slice(0, 3)).join(', ');

  return (
    <Sheet visible={visible} onClose={onClose} title={meal ? `Swap ${meal.recipe.name}` : 'Which meal?'}>
      {!meal ? (
        <View style={{ gap: 12 }}>
          {choices.length === 0 ? (
            <Text variant="bodySmall" color="subtle">
              No meals to swap here.
            </Text>
          ) : null}
          {choices.map((m) => (
            <Pressable
              key={m.id}
              onPress={() => setMeal(m)}
              accessibilityRole="button"
              style={({ pressed }) => [styles.choice, { backgroundColor: colors.surface, opacity: pressed ? 0.8 : 1 }]}
            >
              <View style={{ flex: 1 }}>
                <Text variant="label" color="subtle">
                  {dayLabel(m.dayOfWeek)} {m.slot.toLowerCase()}
                </Text>
                <Text variant="option" color="heading">
                  {m.recipe.name}
                </Text>
              </View>
              <Text variant="label" color="subtle">
                {perServing(m.recipe)}
              </Text>
            </Pressable>
          ))}
        </View>
      ) : (
        <View>
          <Text variant="label" color="subtle">
            {run.length > 1 ? `Changes ${days} (cooked once, then leftovers).` : `Changes ${days}.`}
          </Text>
          <View style={styles.tabs} accessibilityRole="tablist">
            {(['suggestions', 'favorites'] as const).map((t) => (
              <Pressable key={t} onPress={() => setTab(t)} accessibilityRole="tab" accessibilityState={{ selected: tab === t }} style={styles.tab}>
                <Text variant="buttonLarge" color={tab === t ? 'selected' : 'subtle'} style={tab === t && { textDecorationLine: 'underline' }}>
                  {t === 'suggestions' ? 'SUGGESTIONS' : 'FAVORITES'}
                </Text>
              </Pressable>
            ))}
          </View>
          {error ? <ErrorNote message={error} /> : null}
          {!options && !error ? <ActivityIndicator style={{ marginVertical: 24 }} /> : null}
          {options && options.length === 0 ? (
            <Text variant="bodySmall" color="subtle" style={{ marginVertical: 16 }}>
              {tab === 'favorites' ? 'No favorites yet. Tap the heart on any recipe to save it here.' : 'No other recipes fit this meal right now.'}
            </Text>
          ) : null}
          <View style={{ gap: 10 }}>
            {options?.map((r) => {
              const diff = r.costPerServing != null && meal.recipe.costPerServing != null ? r.costPerServing - meal.recipe.costPerServing : null;
              return (
                <Pressable
                  key={r.id}
                  onPress={() => {
                    tap();
                    swap(meal.id, r.id).catch(() => {});
                    onClose();
                  }}
                  accessibilityRole="button"
                  accessibilityLabel={`Swap in ${r.name}, ${perServing(r) ?? ''}`}
                  style={({ pressed }) => [styles.choice, { backgroundColor: colors.surface, opacity: pressed ? 0.8 : 1 }]}
                >
                  <View style={{ flex: 1 }}>
                    <Text variant="option" color="heading">
                      {r.name}
                    </Text>
                    <Text variant="label" color="subtle">
                      {[perServing(r), macroLine(r)].filter(Boolean).join(' · ')}
                    </Text>
                  </View>
                  {diff != null && Math.abs(diff) >= 0.01 ? (
                    <Text variant="label" style={{ fontFamily: fontFamily('ui', 600), color: diff < 0 ? '#2F5A33' : colors.subtle }}>
                      {diff < 0 ? '-' : '+'}
                      {money(Math.abs(diff))}
                    </Text>
                  ) : null}
                </Pressable>
              );
            })}
          </View>
          {choices.length > 1 ? (
            <Text variant="label" style={{ textDecorationLine: 'underline', marginTop: 16 }} onPress={() => setMeal(null)} accessibilityRole="button">
              Pick a different meal
            </Text>
          ) : null}
        </View>
      )}
    </Sheet>
  );
}

/** Dark forest "This week's groceries" card from the Home designs. */
export function BudgetCard({ plan, budget, onPress, onSwap }: { plan: MealPlan | null; budget: number; onPress?: () => void; onSwap?: () => void }) {
  const total = plan?.estimatedTotalCost ?? 0;
  const over = total > budget;
  const diff = Math.abs(budget - total);
  const fill = budget > 0 ? Math.min(1, total / budget) : 0;
  const cream = tokens.palette.backgroundLight;
  const red = '#D9534A';

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={`This week's groceries: ${money(total, true)} of ${money(budget, true)}. ${plan ? (over ? `${money(diff, true)} over budget` : `${money(diff, true)} under budget`) : 'No plan yet'}. Open budget`}
      style={({ pressed }) => [styles.budgetCard, { opacity: pressed ? 0.95 : 1 }]}
    >
      <Text variant="label" style={{ color: '#C9D3CC' }}>
        This week's groceries
      </Text>
      <View style={{ flexDirection: 'row', alignItems: 'baseline', gap: 8, marginTop: 10 }}>
        <Text style={{ fontFamily: fontFamily('ui', 700), fontSize: 38, lineHeight: 46, color: cream }}>
          {money(total, true)}
          {!plan?.costComplete && plan ? '+' : ''}
        </Text>
        <Text variant="option" style={{ color: cream }}>
          of {money(budget, true)}
        </Text>
      </View>
      <View style={styles.budgetTrack}>
        <View style={{ width: `${fill * 100}%`, height: '100%', borderRadius: 4, backgroundColor: over ? red : tokens.palette.sage }} />
      </View>
      {plan ? (
        over ? (
          <Text variant="label" style={{ color: cream, marginTop: 12 }}>
            <Text style={{ fontFamily: fontFamily('ui', 700), color: cream }}>{money(diff, true)} </Text>
            <Text style={{ fontFamily: fontFamily('ui', 700), color: red }}>over</Text>
            <Text style={{ fontFamily: fontFamily('ui', 700), color: cream }}> budget: </Text>
            <Text style={{ color: cream, textDecorationLine: onSwap ? 'underline' : 'none' }} onPress={onSwap} accessibilityRole="button">
              Swap meals?
            </Text>
          </Text>
        ) : (
          <Text variant="label" style={{ color: tokens.palette.sage, marginTop: 12, fontFamily: fontFamily('ui', 600) }}>
            {money(diff, true)} under budget
          </Text>
        )
      ) : (
        <View style={{ height: 32 }} />
      )}
    </Pressable>
  );
}

const UNDER = '#5E9A63';
const OVER = '#A8487A';

/**
 * Weekly spend vs budget (oldest to newest). Bars above the dashed budget line
 * are over budget; color repeats that (validated for color-blind separation).
 * Tap a bar to read its exact numbers.
 */
export function BudgetHistoryChart({ weeks }: { weeks: { id: string; weekStartDate: string; weekEndDate: string; total: number; budget: number }[] }) {
  const { colors } = useTheme();
  const [width, setWidth] = useState(0);
  const [picked, setPicked] = useState<string | null>(null);
  const H = 150;
  const max = Math.max(1, ...weeks.map((w) => Math.max(w.total, w.budget))) * 1.1;
  const gap = 8;
  const barW = weeks.length ? Math.max(8, (width - gap * (weeks.length - 1)) / weeks.length) : 0;
  const y = (v: number) => H - (v / max) * H;
  const current = weeks.find((w) => w.id === picked) ?? weeks[weeks.length - 1];
  const under = weeks.filter((w) => w.total <= w.budget).length;

  return (
    <View
      accessible
      accessibilityLabel={`Weekly grocery spend for the last ${weeks.length} weeks. Under budget ${under} of ${weeks.length} weeks.`}
    >
      {current ? (
        <Text variant="label" color="subtle" style={{ marginBottom: 10 }}>
          {formatRange(current.weekStartDate, current.weekEndDate)}:{' '}
          <Text variant="label" color="heading" style={{ fontFamily: fontFamily('ui', 600) }}>
            {money(current.total)}
          </Text>{' '}
          of {money(current.budget, true)}
          {current.total > current.budget ? ' (over)' : ''}
        </Text>
      ) : null}
      <View onLayout={(e) => setWidth(e.nativeEvent.layout.width)} style={{ height: H }}>
        {width > 0 ? (
          <Svg width={width} height={H}>
            {weeks.map((w, i) => {
              const x = i * (barW + gap);
              const top = y(w.total);
              const sel = current?.id === w.id;
              return (
                <Rect
                  key={w.id}
                  x={x}
                  y={top}
                  width={barW}
                  height={Math.max(2, H - top)}
                  rx={4}
                  fill={w.total > w.budget ? OVER : UNDER}
                  opacity={sel ? 1 : 0.72}
                  onPress={() => setPicked(w.id)}
                />
              );
            })}
            {/* Budget line: weeks can differ if the budget changed, so draw one segment per bar. */}
            {weeks.map((w, i) => (
              <Line key={`b-${w.id}`} x1={i * (barW + gap) - gap / 2} x2={i * (barW + gap) + barW + gap / 2} y1={y(w.budget)} y2={y(w.budget)} stroke={colors.heading} strokeWidth={1.5} strokeDasharray="4 3" />
            ))}
          </Svg>
        ) : null}
      </View>
      <View style={{ flexDirection: 'row', marginTop: 6 }}>
        {weeks.map((w, i) => (
          <Pressable key={w.id} onPress={() => setPicked(w.id)} style={{ width: barW, marginRight: i < weeks.length - 1 ? gap : 0, alignItems: 'center' }} accessibilityLabel={`Week of ${shortDate(w.weekStartDate)}`}>
            <Text style={{ fontSize: 9, lineHeight: 12, color: colors.subtle }} numberOfLines={1}>
              {shortDate(w.weekStartDate)}
            </Text>
          </Pressable>
        ))}
      </View>
      <View style={styles.legend}>
        <Legend color={UNDER} label="Under budget" />
        <Legend color={OVER} label="Over budget" />
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
          <View style={{ width: 16, borderTopWidth: 1.5, borderStyle: 'dashed', borderColor: colors.heading }} />
          <Text variant="fine" color="subtle">
            Budget
          </Text>
        </View>
      </View>
    </View>
  );
}

function Legend({ color, label }: { color: string; label: string }) {
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
      <View style={{ width: 10, height: 10, borderRadius: 3, backgroundColor: color }} />
      <Text variant="fine" color="subtle">
        {label}
      </Text>
    </View>
  );
}

export const SLOT_COLORS = { BREAKFAST: tokens.palette.mauve, LUNCH: tokens.palette.sage, DINNER: tokens.palette.forest } as const;

const styles = StyleSheet.create({
  pill: { minWidth: 82, height: 36, paddingHorizontal: 18, borderRadius: 999, alignItems: 'center', justifyContent: 'center', boxShadow: '0 2px 3px rgba(0, 0, 0, 0.25)' },
  actions: { flexDirection: 'row', justifyContent: 'center', gap: 22, marginTop: 30 },
  choice: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 14, borderRadius: 12, boxShadow: '0 1px 3px rgba(0, 0, 0, 0.15)' },
  tabs: { flexDirection: 'row', gap: 24, marginTop: 16, marginBottom: 14 },
  tab: { paddingVertical: 6 },
  budgetCard: { backgroundColor: tokens.palette.forest, borderRadius: 18, padding: 22, boxShadow: '0 3px 6px rgba(0, 0, 0, 0.25)' },
  budgetTrack: { height: 8, borderRadius: 4, backgroundColor: '#41544A', marginTop: 16, overflow: 'hidden' },
  legend: { flexDirection: 'row', gap: 18, marginTop: 12, flexWrap: 'wrap' },
});

