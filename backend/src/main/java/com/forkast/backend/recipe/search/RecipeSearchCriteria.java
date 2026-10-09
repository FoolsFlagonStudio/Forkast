package com.forkast.backend.recipe.search;

import java.math.BigDecimal;
import java.util.Set;

import com.forkast.backend.diet.DietaryLabel;

/**
 * Everything that narrows a recipe search. Any field may be null (or empty) to
 * mean "no filter".
 * requiredLabels is the user's restrictions plus any labels asked for in the
 * request.
 */
public record RecipeSearchCriteria(
                String text,
                Set<DietaryLabel> requiredLabels,
                BigDecimal minProtein,
                BigDecimal maxCalories,
                Integer maxTotalMinutes,
                BigDecimal maxCostPerServing) {
}