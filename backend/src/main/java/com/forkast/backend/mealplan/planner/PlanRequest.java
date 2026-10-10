package com.forkast.backend.mealplan.planner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.forkast.backend.mealplan.MealSlot;

/**
 * Everything about the user and the week that the generator reads.
 *
 * @param weeklyBudget       null = no budget
 * @param maxCaloriesPerMeal null = no limit; otherwise a hard filter
 * @param locked             selections to keep as they are (regenerate)
 */
public record PlanRequest(
        LocalDate weekStart,
        Map<MealSlot, Integer> mealSlots,
        int household,
        boolean leftoversForLunch,
        boolean prefersMealPrep,
        BigDecimal weeklyBudget,
        Integer maxCaloriesPerMeal,
        int repeatAvoidanceDays,
        long seed,
        List<LockedPick> locked) {

    public PlanRequest {
        mealSlots = Map.copyOf(mealSlots);
        locked = locked == null ? List.of() : List.copyOf(locked);
    }

    /**
     * A selection that keeps its recipe: one the user locked, or one whose days
     * have started.
     * It carries the recipe itself, since a kept recipe needn't pass today's
     * filters.
     */
    public record LockedPick(MealSlot slot, int position, RecipeOption recipe) {
    }
}