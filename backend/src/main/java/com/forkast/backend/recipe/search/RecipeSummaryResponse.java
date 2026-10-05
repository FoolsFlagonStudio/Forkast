package com.forkast.backend.recipe.search;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.forkast.backend.diet.DietaryLabel;
import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.recipe.fit.FitFlag;
import com.forkast.backend.recipe.fit.FitResult;

/** One recipe card in search results and favorites. */
public record RecipeSummaryResponse(
        UUID id,
        String name,
        String imageUrl,
        Integer totalTimeMinutes,
        int baseServings,
        BigDecimal caloriesPerServing,
        BigDecimal proteinGPerServing,
        BigDecimal carbsGPerServing,
        BigDecimal fatGPerServing,
        boolean nutritionComplete,
        int mealPrepScore,
        List<String> labels,
        List<FitFlag> flags,
        boolean favorite) {

    /**
     * recipe must have its labels loaded (RecipeRepository.findWithLabelsByIdIn).
     */
    public static RecipeSummaryResponse from(Recipe recipe, FitResult fit, boolean favorite) {
        return new RecipeSummaryResponse(
                recipe.getId(),
                recipe.getName(),
                recipe.getImageUrl(),
                totalMinutes(recipe),
                recipe.getBaseServings(),
                recipe.getCaloriesPerServing(),
                recipe.getProteinGPerServing(),
                recipe.getCarbsGPerServing(),
                recipe.getFatGPerServing(),
                recipe.isNutritionComplete(),
                recipe.getMealPrepScore(),
                recipe.getDietaryLabels().stream().map(DietaryLabel::getName).sorted().toList(),
                fit.flags(),
                favorite);
    }

    /** prep + cook, or null when the recipe gives neither. */
    public static Integer totalMinutes(Recipe recipe) {
        Integer prep = recipe.getPrepTimeMinutes();
        Integer cook = recipe.getCookTimeMinutes();
        if (prep == null && cook == null) {
            return null;
        }
        return (prep == null ? 0 : prep) + (cook == null ? 0 : cook);
    }
}