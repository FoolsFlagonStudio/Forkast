package com.forkast.backend.mealplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.user.User;

/** Entity rules for plans, selections and calendar rows; no database. */
class MealPlanTest {

    private static final LocalDate MONDAY = LocalDate.parse("2026-10-12");

    private final User user = new User("Test", "User", "plan-test@example.com", "hash");
    private final Recipe chili = new Recipe("Chili", 6);

    private MealPlan plan(String budget) {
        return new MealPlan(user, MONDAY, budget == null ? null : new BigDecimal(budget), PlanSource.MANUAL);
    }

    /** A 3-day dinner: cooked Monday, leftovers Tuesday and Wednesday. */
    private PlanSelection threeDayDinner(MealPlan plan) {
        PlanSelection dinner = new PlanSelection(MealSlot.DINNER, 1, MONDAY, 3);
        dinner.assignRecipe(chili, 6, new BigDecimal("1.000"), new BigDecimal("9.00"));
        plan.addSelection(dinner);
        dinner.addMeal(MONDAY, MealSlot.DINNER, false);
        dinner.addMeal(MONDAY.plusDays(1), MealSlot.DINNER, true);
        dinner.addMeal(MONDAY.plusDays(2), MealSlot.DINNER, true);
        return dinner;
    }

    @Test
    void selectionOwnsItsMealsAndThePlanShowsThemAsACalendar() {
        MealPlan plan = plan("100.00");
        PlanSelection dinner = threeDayDinner(plan);
        PlanSelection lunch = new PlanSelection(MealSlot.LUNCH, 1, MONDAY, 1);
        lunch.assignRecipe(new Recipe("Salad", 2), 2, BigDecimal.ONE, null);
        plan.addSelection(lunch);
        lunch.addMeal(MONDAY, MealSlot.LUNCH, false);

        assertThat(plan.getPlannedMeals())
                .extracting(PlannedMeal::getMealDate, PlannedMeal::getMealSlot, PlannedMeal::isLeftover)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(MONDAY, MealSlot.LUNCH, false),
                        org.assertj.core.groups.Tuple.tuple(MONDAY, MealSlot.DINNER, false),
                        org.assertj.core.groups.Tuple.tuple(MONDAY.plusDays(1), MealSlot.DINNER, true),
                        org.assertj.core.groups.Tuple.tuple(MONDAY.plusDays(2), MealSlot.DINNER, true));
        assertThat(plan.getSelections()).containsExactly(lunch, dinner); // breakfast to dinner
        assertThat(dinner.getMeals()).allSatisfy(m -> assertThat(m.getRecipe()).isSameAs(chili));
        assertThat(dinner.getLastMealDate()).isEqualTo(MONDAY.plusDays(2));
        assertThat(plan.getWeekEndDate()).isEqualTo(LocalDate.parse("2026-10-18"));
    }

    @Test
    void dinnerLeftoversCanBeTheNextDaysLunch() {
        MealPlan plan = plan(null);
        PlanSelection dinner = new PlanSelection(MealSlot.DINNER, 1, MONDAY, 2);
        dinner.assignRecipe(chili, 6, BigDecimal.ONE, null);
        plan.addSelection(dinner);
        dinner.addMeal(MONDAY, MealSlot.DINNER, false);
        PlannedMeal lunch = dinner.addMeal(MONDAY.plusDays(1), MealSlot.LUNCH, true);

        assertThat(lunch.getSelection().getMealSlot()).isEqualTo(MealSlot.DINNER);
        assertThat(lunch.getMealSlot()).isEqualTo(MealSlot.LUNCH);
        assertThat(lunch.getMealPlan()).isSameAs(plan);
    }

    @Test
    void mealsNeedTheSelectionToBeInAPlanFirst() {
        PlanSelection loose = new PlanSelection(MealSlot.DINNER, 1, MONDAY, 1);

        assertThatThrownBy(() -> loose.addMeal(MONDAY, MealSlot.DINNER, false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void overBudgetUsesGroceryCostOnceThereIsOne() {
        MealPlan plan = plan("50.00");

        plan.setCosts(new BigDecimal("45.00"), null, 0);
        assertThat(plan.isOverBudget()).isFalse(); // usage fits, no list yet

        plan.setCosts(new BigDecimal("45.00"), new BigDecimal("53.20"), 0);
        assertThat(plan.isOverBudget()).isTrue(); // the trip doesn't

        plan.setCosts(new BigDecimal("45.00"), new BigDecimal("49.99"), 3);
        assertThat(plan.isOverBudget()).isFalse();
        assertThat(plan.getOpenMeals()).isEqualTo(3);
    }

    @Test
    void noBudgetIsNeverOverBudget() {
        MealPlan plan = plan(null);
        plan.setCosts(new BigDecimal("500.00"), new BigDecimal("600.00"), 0);

        assertThat(plan.isOverBudget()).isFalse();
    }

    @Test
    void selectionRulesAreEnforced() {
        assertThatThrownBy(() -> new PlanSelection(MealSlot.DINNER, 0, MONDAY, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlanSelection(MealSlot.DINNER, 1, MONDAY, 8))
                .isInstanceOf(IllegalArgumentException.class);
        PlanSelection s = new PlanSelection(MealSlot.DINNER, 1, MONDAY, 3);
        assertThatThrownBy(() -> s.assignRecipe(chili, 0, BigDecimal.ONE, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s.assignRecipe(chili, 6, BigDecimal.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startedMeansTheFirstMealIsTodayOrEarlier() {
        PlanSelection s = new PlanSelection(MealSlot.DINNER, 1, MONDAY, 3);

        assertThat(s.hasStarted(MONDAY.minusDays(1))).isFalse();
        assertThat(s.hasStarted(MONDAY)).isTrue();
        assertThat(s.hasStarted(MONDAY.plusDays(2))).isTrue();
    }
}