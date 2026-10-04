package com.forkast.backend.ingest;

import java.math.BigDecimal;

import com.forkast.backend.ingredient.Ingredient;

/**
 * One ingredient line as the calculators see it: what the parser read plus what
 * the
 * matcher found. ingredient is null when the line is unmatched (needs review).
 */
public record RecipeLine(
        Ingredient ingredient,
        BigDecimal amount,
        Unit unit,
        String prepNote,
        boolean optional) {

    /**
     * Lines that count toward nutrition: required, with an amount, and not a pinch
     * or "to taste".
     */
    public boolean countsTowardNutrition() {
        return !optional
                && amount != null
                && unit != null
                && unit != Unit.PINCH
                && unit != Unit.TO_TASTE;
    }
}