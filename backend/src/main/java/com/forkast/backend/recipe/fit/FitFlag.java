package com.forkast.backend.recipe.fit;

/**
 * One way a recipe misses the user's preferences. code is for the app's logic,
 * message for people.
 */
public record FitFlag(Code code, String message) {

    public enum Code {
        OVER_MAX_CALORIES,
        UNDER_PROTEIN_TARGET,
        OVER_CARBS_TARGET,
        OVER_FAT_TARGET,
        NUTRITION_INCOMPLETE
    }
}