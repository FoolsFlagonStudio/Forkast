package com.forkast.backend.mealplan.dto;

import java.math.BigDecimal;

/**
 * A recipe that could replace a selection: what it would cost for that
 * selection's servings,
 * and how that changes the plan's usage cost (negative = cheaper).
 */
public record SuggestionResponse(
        MealPlanDetailResponse.PlanRecipe recipe,
        int servings,
        BigDecimal estimatedCost,
        BigDecimal costChange) {
}