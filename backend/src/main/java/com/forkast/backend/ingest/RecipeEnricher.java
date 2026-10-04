package com.forkast.backend.ingest;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.forkast.backend.diet.DietaryLabel;
import com.forkast.backend.diet.DietaryLabelRepository;
import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.recipe.RecipeIngredient;
import com.forkast.backend.recipe.RecipeStep;

/**
 * Computes everything derived from a recipe's lines: nutrition, dietary labels
 * and the
 * meal-prep score. Used on ingest, and again whenever a reviewed line changes
 * (step 10).
 */
@Component
public class RecipeEnricher {

    private final NutritionCalculator nutritionCalculator;
    private final DietaryLabelClassifier dietaryLabelClassifier;
    private final MealPrepClassifier mealPrepClassifier;
    private final DietaryLabelRepository dietaryLabelRepository;

    public RecipeEnricher(NutritionCalculator nutritionCalculator,
            DietaryLabelClassifier dietaryLabelClassifier,
            MealPrepClassifier mealPrepClassifier,
            DietaryLabelRepository dietaryLabelRepository) {
        this.nutritionCalculator = nutritionCalculator;
        this.dietaryLabelClassifier = dietaryLabelClassifier;
        this.mealPrepClassifier = mealPrepClassifier;
        this.dietaryLabelRepository = dietaryLabelRepository;
    }

    public void enrich(Recipe recipe) {
        List<RecipeLine> lines = recipe.getIngredients().stream()
                .map(RecipeEnricher::toLine)
                .toList();

        NutritionResult nutrition = nutritionCalculator.calculate(lines, recipe.getBaseServings());
        recipe.setNutrition(
                nutrition.caloriesPerServing(),
                nutrition.proteinGPerServing(),
                nutrition.carbsGPerServing(),
                nutrition.fatGPerServing(),
                nutrition.complete());

        replaceLabels(recipe, dietaryLabelClassifier.classify(lines, nutrition));

        recipe.setMealPrepScore(mealPrepClassifier.score(new MealPrepInput(
                recipe.getName(),
                recipe.getCategory(),
                recipe.getKeywords(),
                recipe.getSteps().stream().map(RecipeStep::getInstructionText).toList(),
                recipe.getBaseServings(),
                recipe.getIngredients().stream().map(RecipeEnricher::displayName).toList())));
    }

    // ---------- helpers ----------

    private void replaceLabels(Recipe recipe, Set<String> labelNames) {
        for (DietaryLabel label : List.copyOf(recipe.getDietaryLabels())) {
            recipe.removeDietaryLabel(label);
        }
        dietaryLabelRepository.findAll().stream()
                .filter(label -> labelNames.contains(label.getName()))
                .forEach(recipe::addDietaryLabel);
    }

    private static RecipeLine toLine(RecipeIngredient line) {
        return new RecipeLine(
                line.getIngredient(),
                line.getAmount(),
                toUnit(line.getUnit()),
                line.getPrepNote(),
                line.isOptional());
    }

    /**
     * Units are stored as the enum name in lowercase ("cup", "fl_oz", "to_taste").
     */
    private static Unit toUnit(String stored) {
        try {
            return Unit.valueOf(stored.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private static String displayName(RecipeIngredient line) {
        return line.getIngredient() != null ? line.getIngredient().getName() : line.getParsedName();
    }
}