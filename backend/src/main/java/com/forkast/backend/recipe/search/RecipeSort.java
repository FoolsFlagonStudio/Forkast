package com.forkast.backend.recipe.search;

import java.util.Locale;

import com.forkast.backend.common.exception.ApiException;

/**
 * The orders a search can come back in. FIT and TIME are sorted in Java; the
 * rest in SQL.
 */
public enum RecipeSort {
    FIT,
    PROTEIN,
    CALORIES,
    TIME,
    MEAL_PREP,
    NEWEST;

    /** "mealPrep" and "meal_prep" both work; missing means FIT. */
    public static RecipeSort from(String value) {
        if (value == null || value.isBlank()) {
            return FIT;
        }
        String normalized = value.trim().replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(
                    "Unknown sort '" + value + "'. Use fit, protein, calories, time, mealPrep or newest.");
        }
    }
}