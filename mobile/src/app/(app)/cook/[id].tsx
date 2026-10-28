import { useEffect, useMemo, useRef, useState } from 'react';
import { Platform, Pressable, ScrollView, StyleSheet, Vibration, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { useKeepAwake } from 'expo-keep-awake';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { recipesApi, type RecipeDetail } from '@/api/recipes';
import { useAuth } from '@/auth/AuthProvider';
import { Sheet } from '@/components/app/Sheet';
import { ErrorNote, Loading, tap } from '@/components/app/ui';
import { Button } from '@/components/Button';
import { Text } from '@/components/Text';
import { ingredientText } from '@/lib/format';
import { Icon } from '@/theme/icons';
import { fontFamily } from '@/theme/fonts';
import { useTheme } from '@/theme/themeContext';

/** First time span in a step: "15-20 minutes" -> 15 min, "1 hour" -> 60 min. */
export function stepTimerSeconds(text: string): number | null {
  const m = text.match(/(\d+)(?:\s*(?:-|–|to)\s*\d+)?\s*(hours?|hrs?|minutes?|mins?)\b/i);
  if (!m) return null;
  const n = Number(m[1]);
  return /^h/i.test(m[2]) ? n * 3600 : n * 60;
}

const clock = (s: number) => {
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  return h ? `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}` : `${m}:${String(sec).padStart(2, '0')}`;
};

export default function CookMode() {
  useKeepAwake(); // screen stays on while cooking
  const { id, servings } = useLocalSearchParams<{ id: string; servings?: string }>();
  const { authedRequest } = useAuth();
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();
  const [recipe, setRecipe] = useState<RecipeDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [step, setStep] = useState(0);
  const [done, setDone] = useState(false);
  const [showIngredients, setShowIngredients] = useState(false);

  useEffect(() => {
    recipesApi
      .detail(authedRequest, id, servings ? Number(servings) : undefined)
      .then(setRecipe)
      .catch(() => setError("Couldn't load this recipe."));
  }, [authedRequest, id, servings]);

  const close = () => (router.canGoBack() ? router.back() : router.replace('/home'));

  if (!recipe) {
    return <View style={[styles.screen, { backgroundColor: colors.background, paddingTop: insets.top + 40 }]}>{error ? <ErrorNote message={error} /> : <Loading />}</View>;
  }

  const total = recipe.steps.length;
  const current = recipe.steps[step];

  if (done) return <Done recipe={recipe} onClose={close} />;

  return (
    <View style={[styles.screen, { backgroundColor: colors.background, paddingTop: insets.top + 8, paddingBottom: Math.max(insets.bottom, 16) + 8 }]}>
      <View style={styles.top}>
        <Pressable onPress={close} hitSlop={12} accessibilityRole="button" accessibilityLabel="Exit cook mode">
          <Icon.close size={26} color={colors.text} />
        </Pressable>
        <Text variant="label" color="subtle" numberOfLines={1} style={{ flex: 1, textAlign: 'center' }}>
          {recipe.name}
        </Text>
        <Pressable onPress={() => setShowIngredients(true)} hitSlop={12} accessibilityRole="button" accessibilityLabel="Show ingredients">
          <Icon.groceries size={24} color={colors.text} />
        </Pressable>
      </View>

      <View style={[styles.progress, { backgroundColor: colors.primary }]} accessibilityRole="progressbar" accessibilityLabel={`Step ${step + 1} of ${total}`}>
        <View style={{ width: `${((step + 1) / total) * 100}%`, height: '100%', backgroundColor: colors.selected, borderRadius: 2 }} />
      </View>
      <Text variant="label" color="subtle" style={{ textAlign: 'center', marginTop: 10 }}>
        Step {step + 1} of {total}
      </Text>

      <ScrollView contentContainerStyle={styles.body}>
        <Text variant="body" color="text" style={styles.stepText} accessibilityLiveRegion="polite">
          {current.text}
        </Text>
        <StepTimer key={step} seconds={stepTimerSeconds(current.text)} />
      </ScrollView>

      <View style={styles.footer}>
        <Pressable onPress={() => setStep((s) => s - 1)} disabled={step === 0} hitSlop={12} accessibilityRole="button" accessibilityLabel="Previous step" style={{ opacity: step === 0 ? 0.3 : 1 }}>
          <Text variant="buttonLarge" style={{ color: colors.selected }}>
            PREVIOUS
          </Text>
        </Pressable>
        <Button label={step === total - 1 ? 'Finish' : 'Next'} size="lg" onPress={() => (step === total - 1 ? setDone(true) : setStep((s) => s + 1))} />
      </View>

      <Sheet visible={showIngredients} onClose={() => setShowIngredients(false)} title="Ingredients">
        {recipe.ingredients.map((l, i) => (
          <Text key={i} variant="option" color="text" style={{ paddingVertical: 6 }}>
            {ingredientText(l)}
            {l.prepNote ? `, ${l.prepNote}` : ''}
            {l.optional ? ' (optional)' : ''}
          </Text>
        ))}
      </Sheet>
    </View>
  );
}

/** Countdown for steps that mention a time. Based on the clock, so it stays right if the app is backgrounded. */
function StepTimer({ seconds }: { seconds: number | null }) {
  const { colors } = useTheme();
  const [left, setLeft] = useState(seconds ?? 0);
  const [endsAt, setEndsAt] = useState<number | null>(null);
  const buzzed = useRef(false);

  useEffect(() => {
    if (endsAt == null) return;
    const t = setInterval(() => {
      const remaining = Math.max(0, Math.round((endsAt - Date.now()) / 1000));
      setLeft(remaining);
      if (remaining === 0) {
        setEndsAt(null);
        if (!buzzed.current && Platform.OS !== 'web') Vibration.vibrate([0, 400, 200, 400]);
        buzzed.current = true;
      }
    }, 250);
    return () => clearInterval(t);
  }, [endsAt]);

  if (!seconds) return null;
  const running = endsAt != null;

  return (
    <View style={styles.timer}>
      <Pressable
        onPress={() => {
          tap();
          setEndsAt(null);
          setLeft(seconds);
          buzzed.current = false;
        }}
        accessibilityRole="button"
        accessibilityLabel="Reset timer"
        style={[styles.timerButton, { backgroundColor: colors.primary }]}
      >
        <Icon.resetTimer size={28} color={colors.onAccent} strokeWidth={1.75} />
      </Pressable>
      <View style={{ alignItems: 'center', minWidth: 140 }}>
        <Text style={{ fontFamily: fontFamily('ui', 500), fontSize: 60, lineHeight: 70, color: colors.textStrong }} accessibilityLabel={`Timer ${clock(left)}`}>
          {clock(left)}
        </Text>
        {left === 0 ? (
          <Text variant="label" color="danger" accessibilityLiveRegion="assertive">
            Time's up
          </Text>
        ) : null}
      </View>
      <Pressable
        onPress={() => {
          tap();
          if (running) {
            setEndsAt(null);
          } else {
            const from = left === 0 ? seconds : left;
            setLeft(from);
            buzzed.current = false;
            setEndsAt(Date.now() + from * 1000);
          }
        }}
        accessibilityRole="button"
        accessibilityLabel={running ? 'Pause timer' : 'Start timer'}
        style={[styles.timerButton, { backgroundColor: colors.primary }]}
      >
        {running ? <Icon.pause size={28} color={colors.onAccent} strokeWidth={1.75} /> : <Icon.play size={28} color={colors.onAccent} strokeWidth={1.75} />}
      </Pressable>
    </View>
  );
}

const RATINGS_KEY = 'victual.ratings';

/** Finished screen with a quick thumbs up/down. Ratings stay on the device until the backend stores them. */
function Done({ recipe, onClose }: { recipe: RecipeDetail; onClose: () => void }) {
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();
  const [rating, setRating] = useState<'up' | 'down' | null>(null);
  const rate = useMemo(
    () => async (r: 'up' | 'down') => {
      tap();
      setRating(r);
      try {
        const all = JSON.parse((await AsyncStorage.getItem(RATINGS_KEY)) ?? '{}');
        await AsyncStorage.setItem(RATINGS_KEY, JSON.stringify({ ...all, [recipe.id]: r }));
      } catch {
        /* best effort */
      }
    },
    [recipe.id],
  );

  return (
    <View style={[styles.screen, styles.done, { backgroundColor: colors.background, paddingTop: insets.top }]}>
      <Icon.done size={72} color={colors.primary} strokeWidth={1.5} />
      <Text variant="title" color="heading" style={{ textAlign: 'center', marginTop: 24 }}>
        Enjoy your meal!
      </Text>
      <Text variant="lead" color="subtle" style={{ textAlign: 'center', marginTop: 10 }}>
        How was the {recipe.name.toLowerCase()}?
      </Text>
      <View style={{ flexDirection: 'row', gap: 26, marginTop: 26 }}>
        {(['up', 'down'] as const).map((r) => {
          const Glyph = r === 'up' ? Icon.rateUp : Icon.rateDown;
          const on = rating === r;
          return (
            <Pressable
              key={r}
              onPress={() => rate(r)}
              accessibilityRole="radio"
              accessibilityState={{ selected: on }}
              accessibilityLabel={r === 'up' ? 'Loved it' : 'Not for me'}
              style={[styles.rate, { backgroundColor: on ? colors.selected : colors.primary }]}
            >
              <Glyph size={28} color={on ? colors.onSelected : colors.onAccent} strokeWidth={1.75} />
            </Pressable>
          );
        })}
      </View>
      {rating ? (
        <Text variant="label" color="subtle" style={{ marginTop: 14 }}>
          {rating === 'up' ? "Noted. We'll suggest it again." : "Thanks. We'll suggest it less."}
        </Text>
      ) : null}
      <Button label="Done" size="lg" onPress={onClose} style={{ marginTop: 40 }} />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, paddingHorizontal: 24 },
  top: { flexDirection: 'row', alignItems: 'center', gap: 12, height: 44 },
  progress: { height: 4, borderRadius: 2, marginTop: 44, marginHorizontal: 40, overflow: 'hidden' },
  body: { flexGrow: 1, paddingTop: 48, paddingHorizontal: 12 },
  stepText: { textAlign: 'center', fontSize: 19, lineHeight: 28 },
  timer: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 22, marginTop: 40 },
  timerButton: { width: 56, height: 52, borderRadius: 10, alignItems: 'center', justifyContent: 'center' },
  footer: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 14 },
  done: { alignItems: 'center', justifyContent: 'center' },
  rate: { width: 64, height: 64, borderRadius: 32, alignItems: 'center', justifyContent: 'center' },
});
