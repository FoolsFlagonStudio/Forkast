package com.forkast.backend.ingest;

import java.util.List;

/**
 * The recipe fields the meal-prep score looks at. Any text field may be null.
 */
public record MealPrepInput(
        String name,
        String category,
        String keywords,
        List<String> instructions,
        int baseServings,
        List<String> ingredientNames) {
}