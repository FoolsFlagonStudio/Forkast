package com.forkast.backend.recipe.fit;

import java.math.BigDecimal;

import com.forkast.backend.recipe.Recipe;

/**
 * A recipe's per-serving numbers: all the evaluator needs, so tests don't have
 * to build a Recipe.
 */
public record RecipeNutrition(
        BigDecimal calories,
        BigDecimal protein,
        BigDecimal carbs,
        BigDecimal fat,
        boolean complete) {

    public static RecipeNutrition of(Recipe recipe) {
        return new RecipeNutrition(
                recipe.getCaloriesPerServing(),
                recipe.getProteinGPerServing(),
                recipe.getCarbsGPerServing(),
                recipe.getFatGPerServing(),
                recipe.isNutritionComplete());
    }
}