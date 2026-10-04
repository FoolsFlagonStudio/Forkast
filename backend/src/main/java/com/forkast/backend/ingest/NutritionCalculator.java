package com.forkast.backend.ingest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.forkast.backend.ingredient.Ingredient;

/**
 * Per-serving macros: sum of grams x value per 100 g / 100 over required lines,
 * divided by servings.
 */
@Component
public class NutritionCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final UnitConverter unitConverter;

    public NutritionCalculator(UnitConverter unitConverter) {
        this.unitConverter = unitConverter;
    }

    public NutritionResult calculate(List<RecipeLine> lines, int baseServings) {
        if (baseServings < 1) {
            throw new IllegalArgumentException("baseServings must be at least 1");
        }

        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        boolean complete = true;

        for (RecipeLine line : lines) {
            if (!line.countsTowardNutrition()) {
                continue;
            }
            Ingredient ingredient = line.ingredient();
            Optional<BigDecimal> grams = unitConverter.toGrams(line.amount(), line.unit(), line.prepNote(), ingredient);
            if (grams.isEmpty() || !hasNutrients(ingredient)) {
                complete = false; // unmatched, no portion for that unit, or no FDC values
                continue;
            }

            BigDecimal factor = grams.get().divide(HUNDRED);
            calories = calories.add(factor.multiply(ingredient.getCaloriesPer100g()));
            protein = protein.add(factor.multiply(ingredient.getProteinGPer100g()));
            carbs = carbs.add(factor.multiply(ingredient.getCarbsGPer100g()));
            fat = fat.add(factor.multiply(ingredient.getFatGPer100g()));
        }

        BigDecimal servings = BigDecimal.valueOf(baseServings);
        return new NutritionResult(
                perServing(calories, servings),
                perServing(protein, servings),
                perServing(carbs, servings),
                perServing(fat, servings),
                complete);
    }

    private static boolean hasNutrients(Ingredient ingredient) {
        return ingredient != null
                && ingredient.getCaloriesPer100g() != null
                && ingredient.getProteinGPer100g() != null
                && ingredient.getCarbsGPer100g() != null
                && ingredient.getFatGPer100g() != null;
    }

    private static BigDecimal perServing(BigDecimal total, BigDecimal servings) {
        return total.divide(servings, 2, RoundingMode.HALF_UP);
    }
}