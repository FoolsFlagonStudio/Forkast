package com.forkast.backend.recipe.fit;

import com.forkast.backend.user.UserPreferences;

/**
 * The per-meal limits a recipe is compared against. Every value may be null
 * (not set).
 * Protein, carbs and fat are per meal: many users plan only some meals, so
 * there's no
 * daily total to split.
 */
public record FitTargets(
        Integer maxCaloriesPerMeal,
        Integer proteinGrams,
        Integer carbsGrams,
        Integer fatGrams) {

    public static final FitTargets NONE = new FitTargets(null, null, null, null);

    /** A user who hasn't set up preferences yet gets no flags. */
    public static FitTargets from(UserPreferences preferences) {
        if (preferences == null) {
            return NONE;
        }
        return new FitTargets(
                preferences.getMaxCaloriesPerMeal(),
                preferences.getProteinTargetGrams(),
                preferences.getCarbsTargetGrams(),
                preferences.getFatTargetGrams());
    }
}