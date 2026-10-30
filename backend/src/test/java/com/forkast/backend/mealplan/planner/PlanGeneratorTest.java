package com.forkast.backend.mealplan.planner;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.forkast.backend.ingest.SlotScores;
import com.forkast.backend.mealplan.MealSlot;

class PlanGeneratorTest {

    private static final LocalDate SUNDAY = LocalDate.parse("2026-10-11");
    private static final SlotScores DINNER = new SlotScores(5, 40, 90);
    private static final SlotScores BREAKFAST = new SlotScores(90, 25, 10);

    private final PlanGenerator generator = new PlanGenerator(PlannerProperties.defaults());

    // ---------- helpers ----------

    private static UUID id(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes());
    }

    private static RecipeOption recipe(String name, SlotScores slots, String cost) {
        return new RecipeOption(id(name), 4, slots, new BigDecimal("500"), 50,
                cost == null ? null : new BigDecimal(cost), true, 0, false, null);
    }

    private static RecipeOption dinner(String name, String cost) {
        return recipe(name, DINNER, cost);
    }

    private static RecipeOption withCalories(RecipeOption r, String calories) {
        return new RecipeOption(r.id(), r.baseServings(), r.slots(), calories == null ? null : new BigDecimal(calories),
                r.mealPrepScore(), r.costPerServing(), r.costComplete(), r.fitPenalty(), r.favorite(), r.lastServed());
    }

    private static RecipeOption withMealPrep(RecipeOption r, int mealPrep) {
        return new RecipeOption(r.id(), r.baseServings(), r.slots(), r.calories(), mealPrep, r.costPerServing(),
                r.costComplete(), r.fitPenalty(), r.favorite(), r.lastServed());
    }

    private static RecipeOption servedOn(RecipeOption r, LocalDate date) {
        return new RecipeOption(r.id(), r.baseServings(), r.slots(), r.calories(), r.mealPrepScore(),
                r.costPerServing(), r.costComplete(), r.fitPenalty(), r.favorite(), date);
    }

    private static RecipeOption favorite(RecipeOption r) {
        return new RecipeOption(r.id(), r.baseServings(), r.slots(), r.calories(), r.mealPrepScore(),
                r.costPerServing(), r.costComplete(), r.fitPenalty(), true, r.lastServed());
    }

    private static List<RecipeOption> dinners(int count, String cost) {
        List<RecipeOption> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(dinner("Dinner " + i, cost));
        }
        return list;
    }

    /**
     * Dinners only, household 1, no budget, no calorie limit, 14-day repeat window.
     */
    private static PlanRequest request(int dinners, long seed) {
        return new PlanRequest(SUNDAY, Map.of(MealSlot.DINNER, dinners), 1, false, false, null, null, 14, seed,
                List.of());
    }

    private static PlanRequest withBudget(PlanRequest r, String budget) {
        return new PlanRequest(r.weekStart(), r.mealSlots(), r.household(), r.leftoversForLunch(),
                r.prefersMealPrep(), new BigDecimal(budget), r.maxCaloriesPerMeal(), r.repeatAvoidanceDays(), r.seed(),
                r.locked());
    }

    private static List<UUID> recipeIds(GeneratedPlan plan) {
        return plan.picks().stream().map(p -> p.recipe().id()).toList();
    }

    // ---------- determinism and variety ----------

    @Test
    void theSameSeedGivesTheSamePlan() {
        List<RecipeOption> recipes = dinners(20, "3.00");

        GeneratedPlan first = generator.generate(request(7, 42), recipes);
        GeneratedPlan again = generator.generate(request(7, 42), recipes);

        assertThat(recipeIds(again)).isEqualTo(recipeIds(first));
    }

    @Test
    void differentSeedsGiveDifferentPlans() {
        List<RecipeOption> recipes = dinners(20, "3.00");

        assertThat(recipeIds(generator.generate(request(7, 1), recipes)))
                .isNotEqualTo(recipeIds(generator.generate(request(7, 2), recipes)));
    }

    @Test
    void aRecipeIsUsedOnceAWeek() {
        GeneratedPlan plan = generator.generate(request(7, 3), dinners(7, "3.00"));

        assertThat(plan.picks()).hasSize(7);
        assertThat(new HashSet<>(recipeIds(plan))).hasSize(7);
        assertThat(plan.openMeals()).isEqualTo(0);
        assertThat(plan.shortfalls()).isEmpty();
    }

    // ---------- hard filters ----------

    @Test
    void recipesOverTheCalorieLimitOrWithUnknownCaloriesAreNeverChosen() {
        List<RecipeOption> recipes = new ArrayList<>(dinners(3, "3.00"));
        RecipeOption heavy = withCalories(dinner("Heavy", "3.00"), "900");
        RecipeOption unknown = withCalories(dinner("Unknown", "3.00"), null);
        recipes.add(heavy);
        recipes.add(unknown);
        PlanRequest base = request(3, 0);

        for (long seed = 0; seed < 20; seed++) {
            PlanRequest limited = new PlanRequest(SUNDAY, base.mealSlots(), 1, false, false, null, 600, 14, seed,
                    List.of());
            List<UUID> chosen = recipeIds(generator.generate(limited, recipes));
            assertThat(chosen).hasSize(3);
            assertThat(chosen.contains(heavy.id())).isFalse();
            assertThat(chosen.contains(unknown.id())).isFalse();
        }
    }

    @Test
    void recipesWithNoCostAreNeverChosen() {
        List<RecipeOption> recipes = new ArrayList<>(dinners(3, "3.00"));
        RecipeOption unpriced = dinner("Unpriced", null);
        recipes.add(unpriced);

        for (long seed = 0; seed < 20; seed++) {
            List<UUID> chosen = recipeIds(generator.generate(request(3, seed), recipes));
            assertThat(chosen).hasSize(3);
            assertThat(chosen.contains(unpriced.id())).isFalse();
        }
    }

    @Test
    void onlyRecipesThatKeepFillAStretchedRun() {
        // 2 dinners a week: a 4-day stretched run, then a 3-day run
        RecipeOption soup = withMealPrep(dinner("Soup", "3.00"), 85);
        List<RecipeOption> recipes = List.of(dinner("Tacos", "3.00"), dinner("Stir fry", "3.00"), soup);

        for (long seed = 0; seed < 20; seed++) {
            GeneratedPlan plan = generator.generate(request(2, seed), recipes);
            GeneratedPlan.Pick stretched = plan.picks().stream().filter(p -> p.run().stretched()).findFirst()
                    .orElseThrow();
            assertThat(stretched.recipe().id()).isEqualTo(soup.id());
            assertThat(plan.picks()).hasSize(2);
        }
    }

    @Test
    void eachSlotOnlyTakesRecipesThatFitIt() {
        List<RecipeOption> recipes = new ArrayList<>(dinners(4, "3.00"));
        for (int i = 1; i <= 4; i++) {
            recipes.add(recipe("Breakfast " + i, BREAKFAST, "1.50"));
        }
        PlanRequest both = new PlanRequest(SUNDAY, Map.of(MealSlot.BREAKFAST, 3, MealSlot.DINNER, 3), 1, false,
                false, null, null, 14, 9, List.of());

        GeneratedPlan plan = generator.generate(both, recipes);

        assertThat(plan.picks()).hasSize(6);
        for (GeneratedPlan.Pick pick : plan.picks()) {
            assertThat(pick.recipe().slots().fits(pick.run().slot())).isTrue();
        }
    }

    // ---------- repeat window ----------

    @Test
    void recentlyServedRecipesWaitOutTheRepeatWindow() {
        RecipeOption lastWeek = servedOn(dinner("Last week", "3.00"), SUNDAY.minusDays(3));
        List<RecipeOption> recipes = new ArrayList<>(dinners(3, "3.00"));
        recipes.add(lastWeek);

        for (long seed = 0; seed < 20; seed++) {
            assertThat(recipeIds(generator.generate(request(3, seed), recipes)).contains(lastWeek.id())).isFalse();
        }
    }

    @Test
    void whenTooFewRecipesAreLeftTheOldestServedComeBackFirst() {
        RecipeOption tenDaysAgo = servedOn(dinner("Ten days ago", "3.00"), SUNDAY.minusDays(10));
        RecipeOption fiveDaysAgo = servedOn(dinner("Five days ago", "3.00"), SUNDAY.minusDays(5));
        List<RecipeOption> recipes = new ArrayList<>(dinners(2, "3.00"));
        recipes.add(fiveDaysAgo);
        recipes.add(tenDaysAgo);

        GeneratedPlan plan = generator.generate(request(3, 4), recipes);

        assertThat(plan.picks()).hasSize(3);
        assertThat(recipeIds(plan).contains(tenDaysAgo.id())).isTrue();
        assertThat(recipeIds(plan).contains(fiveDaysAgo.id())).isFalse();
    }

    // ---------- shortfalls ----------

    @Test
    void runsNothingCanFillAreLeftOpen() {
        // 3 dinners: runs of 3, 2 and 2 days, but only 2 recipes
        GeneratedPlan plan = generator.generate(request(3, 6), dinners(2, "3.00"));

        assertThat(plan.picks()).hasSize(2);
        assertThat(plan.shortfalls()).hasSize(1);
        GeneratedPlan.Shortfall shortfall = plan.shortfalls().get(0);
        assertThat(shortfall.slot()).isEqualTo(MealSlot.DINNER);
        assertThat(shortfall.position()).isEqualTo(3);
        assertThat(shortfall.meals()).isEqualTo(2);
        assertThat(plan.openMeals()).isEqualTo(2);
    }

    // ---------- budget ----------

    @Test
    void aTightBudgetSwapsInCheaperRecipes() {
        List<RecipeOption> recipes = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            recipes.add(favorite(dinner("Steak " + i, "10.00")));
            recipes.add(dinner("Beans " + i, "2.00"));
        }
        // 7 one-day dinners for 1: the budget target is 30 x 0.90 = 27.00
        PlanRequest tight = withBudget(request(7, 8), "30.00");

        for (long seed = 0; seed < 10; seed++) {
            GeneratedPlan plan = generator.generate(
                    new PlanRequest(SUNDAY, tight.mealSlots(), 1, false, false, tight.weeklyBudget(), null, 14, seed,
                            List.of()),
                    recipes);
            assertThat(plan.fitsBudget()).isTrue();
            assertThat(plan.usageCost()).isLessThanOrEqualTo(new BigDecimal("27.00"));
            assertThat(plan.picks()).hasSize(7);
        }
    }

    @Test
    void aBudgetNothingCanMeetIsFlaggedNotForced() {
        List<RecipeOption> recipes = new ArrayList<>(dinners(7, "2.00"));
        recipes.add(dinner("Steak", "10.00"));

        GeneratedPlan plan = generator.generate(withBudget(request(7, 2), "5.00"), recipes);

        assertThat(plan.fitsBudget()).isFalse();
        assertThat(plan.picks()).hasSize(7);
        assertThat(recipeIds(plan).contains(id("Steak"))).isFalse(); // swapped out on the way down
        assertThat(plan.usageCost()).isEqualByComparingTo("14.00");
    }

    @Test
    void lockedPicksAreKeptEvenOverBudget() {
        RecipeOption steak = dinner("Steak", "10.00");
        List<RecipeOption> recipes = new ArrayList<>(dinners(10, "2.00"));
        recipes.add(steak);
        PlanRequest locked = new PlanRequest(SUNDAY, Map.of(MealSlot.DINNER, 7), 1, false, false,
                new BigDecimal("10.00"), null, 14, 11, List.of(new PlanRequest.LockedPick(MealSlot.DINNER, 2, steak)));

        GeneratedPlan plan = generator.generate(locked, recipes);

        GeneratedPlan.Pick second = plan.picks().stream().filter(p -> p.run().position() == 2).findFirst()
                .orElseThrow();
        assertThat(second.recipe().id()).isEqualTo(steak.id());
        assertThat(second.locked()).isTrue();
        assertThat(recipeIds(plan).stream().filter(steak.id()::equals).count()).isEqualTo(1L);
        assertThat(plan.fitsBudget()).isFalse();
    }

    // ---------- servings ----------

    @Test
    void servingsCoverTheHouseholdForEveryDayOfTheRun() {
        // household 2, 3 dinners: the first run is 3 days, so 6 servings of a 4-serving
        // recipe
        PlanRequest two = new PlanRequest(SUNDAY, Map.of(MealSlot.DINNER, 3), 2, false, false, null, null, 14, 1,
                List.of());

        GeneratedPlan plan = generator.generate(two, dinners(3, "2.50"));

        GeneratedPlan.Pick first = plan.picks().get(0);
        assertThat(first.run().dates()).hasSize(3);
        assertThat(first.servings()).isEqualTo(6);
        assertThat(first.portionMultiplier()).isEqualByComparingTo("1.500");
        assertThat(first.cost()).isEqualByComparingTo("15.00");
        assertThat(plan.usageCost()).isEqualByComparingTo("35.00"); // 14 servings x 2.50
        assertThat(plan.fitsBudget()).isTrue(); // no budget set
    }

    @Test
    void aLeftoverLunchAddsServingsToTheDinner() {
        PlanRequest leftovers = new PlanRequest(SUNDAY, Map.of(MealSlot.DINNER, 7, MealSlot.LUNCH, 7), 1, true,
                false, null, null, 14, 1, List.of());
        List<RecipeOption> recipes = new ArrayList<>(dinners(7, "3.00"));
        for (int i = 1; i <= 7; i++) {
            recipes.add(recipe("Lunch " + i, new SlotScores(5, 85, 20), "2.00"));
        }

        GeneratedPlan plan = generator.generate(leftovers, recipes);

        GeneratedPlan.Pick dinnerWithLunch = plan.picks().stream()
                .filter(p -> p.run().slot() == MealSlot.DINNER && p.run().leftoverLunch() != null).findFirst()
                .orElseThrow();
        assertThat(dinnerWithLunch.servings()).isEqualTo(2);
    }

    // ---------- existing plans: fill, suggest, rejection ----------

    private static SlotRun run(int position, int startOffset, int days, boolean stretched) {
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            dates.add(SUNDAY.plusDays(startOffset + i));
        }
        return new SlotRun(MealSlot.DINNER, position, dates, null, stretched);
    }

    @Test
    void fillRedrawsOnlyTheRunsThatArentLocked() {
        WeekShape shape = new WeekShape(List.of(run(1, 0, 3, false), run(2, 3, 2, false), run(3, 5, 2, false)), 0);
        RecipeOption kept = dinner("Kept", "3.00");
        List<RecipeOption> recipes = new ArrayList<>(dinners(10, "3.00"));
        PlanRequest request = new PlanRequest(SUNDAY, Map.of(MealSlot.DINNER, 3), 1, false, false, null, null, 14,
                99, List.of(new PlanRequest.LockedPick(MealSlot.DINNER, 1, kept)));

        GeneratedPlan plan = generator.fill(shape, request, recipes);

        assertThat(plan.picks()).hasSize(3);
        assertThat(plan.picks().get(0).recipe().id()).isEqualTo(kept.id()); // kept, though not in the recipe list
        assertThat(plan.picks().get(0).locked()).isTrue();
        assertThat(plan.picks().get(1).locked()).isFalse();
        assertThat(new HashSet<>(recipeIds(plan))).hasSize(3);
    }

    @Test
    void suggestionsAreBestFirstAndLeaveOutThePlansRecipes() {
        SlotRun run = run(1, 0, 2, false);
        RecipeOption inPlan = dinner("In plan", "3.00");
        RecipeOption best = favorite(dinner("Best", "3.00"));
        List<RecipeOption> recipes = new ArrayList<>(dinners(6, "3.00"));
        recipes.add(inPlan);
        recipes.add(best);

        List<PlanGenerator.Suggestion> suggestions = generator.suggest(run, request(3, 1), recipes,
                java.util.Set.of(inPlan.id()), BigDecimal.ZERO, 5);

        assertThat(suggestions).hasSize(5);
        assertThat(suggestions.get(0).recipe().id()).isEqualTo(best.id()); // the favorite boost
        assertThat(suggestions.stream().map(s -> s.recipe().id()).toList().contains(inPlan.id())).isFalse();
        assertThat(suggestions.get(0).cost()).isEqualByComparingTo("6.00"); // 2 servings x 3.00
    }

    @Test
    void suggestionsStayWithinTheBudgetLeft() {
        SlotRun run = run(1, 0, 1, false);
        List<RecipeOption> recipes = List.of(dinner("Cheap", "2.00"), dinner("Pricey", "9.00"));
        // budget 20 x 0.90 = 18.00 target; the other meals already use 14.00, leaving
        // 4.00
        PlanRequest budget = withBudget(request(1, 1), "20.00");

        List<PlanGenerator.Suggestion> suggestions = generator.suggest(run, budget, recipes, java.util.Set.of(),
                new BigDecimal("14.00"), 5);

        assertThat(suggestions.stream().map(s -> s.recipe().id()).toList()).containsExactly(id("Cheap"));
    }

    @Test
    void whenNothingFitsTheBudgetTheCheapestAreSuggested() {
        SlotRun run = run(1, 0, 1, false);
        List<RecipeOption> recipes = List.of(dinner("Pricey", "9.00"), dinner("Less pricey", "6.00"));
        PlanRequest budget = withBudget(request(1, 1), "10.00");

        List<PlanGenerator.Suggestion> suggestions = generator.suggest(run, budget, recipes, java.util.Set.of(),
                new BigDecimal("8.00"), 5);

        assertThat(suggestions.stream().map(s -> s.recipe().id()).toList())
                .containsExactly(id("Less pricey"), id("Pricey"));
    }

    @Test
    void aChosenRecipeMustPassTheHardFilters() {
        PlanRequest limited = new PlanRequest(SUNDAY, Map.of(MealSlot.DINNER, 2), 1, false, false, null, 600, 14, 1,
                List.of());

        assertThat(generator.rejection(run(1, 0, 3, false), dinner("Fine", "3.00"), limited)).isEmpty();
        assertThat(generator.rejection(run(1, 0, 3, false), recipe("Pancakes", BREAKFAST, "1.00"), limited).isPresent())
                .isTrue();
        assertThat(generator.rejection(run(1, 0, 3, false), dinner("Unpriced", null), limited).isPresent()).isTrue();
        assertThat(generator.rejection(run(1, 0, 3, false), withCalories(dinner("Heavy", "3.00"), "900"), limited)
                .isPresent()).isTrue();
        assertThat(generator.rejection(run(1, 0, 4, true), dinner("Tacos", "3.00"), limited).isPresent()).isTrue();
        assertThat(generator.rejection(run(1, 0, 4, true), withMealPrep(dinner("Soup", "3.00"), 80), limited))
                .isEmpty();
    }
}