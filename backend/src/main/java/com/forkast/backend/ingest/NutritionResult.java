package com.forkast.backend.ingest;

import java.math.BigDecimal;

/**
 * Macros per serving. complete is false when any required line couldn't be
 * converted to
 * grams or its ingredient has no nutrient values; the numbers then cover only
 * the lines
 * that could be counted.
 */
public record NutritionResult(
        BigDecimal caloriesPerServing,
        BigDecimal proteinGPerServing,
        BigDecimal carbsGPerServing,
        BigDecimal fatGPerServing,
        boolean complete) {
}