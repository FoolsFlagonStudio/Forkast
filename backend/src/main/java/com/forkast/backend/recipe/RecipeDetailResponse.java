package com.forkast.backend.recipe;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.forkast.backend.recipe.fit.FitFlag;

/**
 * One recipe with steps and ingredients, scaled to `servings`. Nutrition and
 * costPerServing
 * stay per serving whatever `servings` is; estimatedCost is costPerServing x
 * servings, what
 * this many servings uses (not what the shopping trip costs, which buys whole
 * packages).
 */
public record RecipeDetailResponse(
                UUID id,
                String name,
                String description,
                String imageUrl,
                Integer prepTimeMinutes,
                Integer cookTimeMinutes,
                Integer totalTimeMinutes,
                int baseServings,
                int servings,
                String category,
                String cuisine,
                String sourceUrl,
                String sourceHost,
                BigDecimal caloriesPerServing,
                BigDecimal proteinGPerServing,
                BigDecimal carbsGPerServing,
                BigDecimal fatGPerServing,
                boolean nutritionComplete,
                BigDecimal costPerServing,
                BigDecimal estimatedCost,
                boolean costComplete,
                int mealPrepScore,
                List<String> labels,
                List<FitFlag> flags,
                boolean favorite,
                List<IngredientLine> ingredients,
                List<Step> steps) {

        /**
         * name is the matched ingredient's name, or what the parser read for an
         * unmatched line.
         * unit is the stored unit code ("cup", "fl_oz", "count", "to_taste"); the app
         * picks the wording.
         */
        public record IngredientLine(
                        String name,
                        BigDecimal amount,
                        String displayAmount,
                        String unit,
                        String prepNote,
                        boolean optional,
                        String rawText) {
        }

        public record Step(int stepNumber, String text) {
        }
}