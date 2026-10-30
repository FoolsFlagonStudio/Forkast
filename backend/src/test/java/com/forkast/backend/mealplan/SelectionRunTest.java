package com.forkast.backend.mealplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.forkast.backend.mealplan.planner.SlotRun;
import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.user.User;

/**
 * A saved selection read back as the run it fills, which is what editing hands
 * the generator.
 */
class SelectionRunTest {

    private static final LocalDate SUNDAY = LocalDate.parse("2026-10-11");

    private static PlanSelection dinner(int days, boolean leftoverLunch) {
        MealPlan plan = new MealPlan(new User("A", "B", "a@example.com", "h"), SUNDAY, null, PlanSource.MANUAL);
        PlanSelection selection = new PlanSelection(MealSlot.DINNER, 1, SUNDAY, days);
        selection.assignRecipe(new Recipe("Chili", 6), 6, BigDecimal.ONE, null);
        plan.addSelection(selection);
        for (int i = 0; i < days; i++) {
            selection.addMeal(SUNDAY.plusDays(i), MealSlot.DINNER, i > 0);
        }
        if (leftoverLunch) {
            selection.addMeal(SUNDAY.plusDays(1), MealSlot.LUNCH, true);
        }
        return selection;
    }

    @Test
    void theRunHasTheSlotsDatesInOrderAndTheLeftoverLunch() {
        SlotRun run = MealPlanEditService.runOf(dinner(3, true), 3);

        assertThat(run.slot()).isEqualTo(MealSlot.DINNER);
        assertThat(run.position()).isEqualTo(1);
        assertThat(run.dates()).containsExactly(SUNDAY, SUNDAY.plusDays(1), SUNDAY.plusDays(2));
        assertThat(run.leftoverLunch()).isEqualTo(SUNDAY.plusDays(1));
        assertThat(run.meals()).isEqualTo(4);
        assertThat(run.stretched()).isFalse();
    }

    @Test
    void aRunPastTheFreshnessLimitIsStretched() {
        SlotRun run = MealPlanEditService.runOf(dinner(4, false), 3);

        assertThat(run.stretched()).isTrue();
        assertThat(run.leftoverLunch()).isNull();
    }
}