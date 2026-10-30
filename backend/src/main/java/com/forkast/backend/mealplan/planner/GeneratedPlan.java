package com.forkast.backend.mealplan.planner;

import java.math.BigDecimal;
import java.util.List;

import com.forkast.backend.mealplan.MealSlot;

/**
 * What the generator chose. picks cover the runs it could fill; openMeals
 * counts slot-days
 * nothing covers (from the layout, or runs with no recipe left to choose).
 * usageCost is what
 * the chosen meals use; fitsBudget is false when even the cheapest swaps
 * couldn't bring it
 * under the budget margin.
 */
public record GeneratedPlan(
        List<Pick> picks,
        int openMeals,
        List<Shortfall> shortfalls,
        BigDecimal usageCost,
        boolean fitsBudget) {

    public GeneratedPlan {
        picks = List.copyOf(picks);
        shortfalls = List.copyOf(shortfalls);
    }

    /**
     * One filled run. portionMultiplier = servings / the recipe's base servings.
     */
    public record Pick(SlotRun run, RecipeOption recipe, int servings, BigDecimal portionMultiplier,
            BigDecimal cost, boolean locked) {
    }

    /** A run left empty, and why. */
    public record Shortfall(MealSlot slot, int position, int meals, String reason) {
    }
}