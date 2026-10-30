package com.forkast.backend.mealplan.planner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What the generator knows about one recipe that passed the hard filters for a
 * slot.
 *
 * @param slotScore  0 to 100 for the slot being filled
 * @param fitPenalty from RecipeFitEvaluator against the user's per-meal
 *                   targets; 0 = fits
 * @param lastServed the most recent time the user ate it, or null
 */
public record Candidate(
        UUID recipeId,
        int baseServings,
        int slotScore,
        double fitPenalty,
        int mealPrepScore,
        BigDecimal costPerServing,
        boolean costComplete,
        boolean favorite,
        LocalDate lastServed) {
}