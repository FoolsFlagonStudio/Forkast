package com.forkast.backend.ingest;

import java.math.BigDecimal;
import java.util.Set;

import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientPortion;
import com.forkast.backend.ingredient.IngredientTag;

/**
 * Builds in-memory ingredients for unit tests. Nothing is saved; no database is
 * involved.
 */
final class TestIngredients {

    private TestIngredients() {
    }

    /**
     * portions alternate description and grams: portions("cup", "125", "tbsp",
     * "8").
     */
    static Ingredient ingredient(String name, String calories, String protein, String carbs, String fat,
            Set<IngredientTag> tags, String... portions) {
        Ingredient ingredient = new Ingredient(name);
        ingredient.setCaloriesPer100g(calories == null ? null : new BigDecimal(calories));
        ingredient.setProteinGPer100g(protein == null ? null : new BigDecimal(protein));
        ingredient.setCarbsGPer100g(carbs == null ? null : new BigDecimal(carbs));
        ingredient.setFatGPer100g(fat == null ? null : new BigDecimal(fat));
        ingredient.replaceTags(tags);
        for (int i = 0; i < portions.length; i += 2) {
            ingredient.addPortion(new IngredientPortion(portions[i], new BigDecimal(portions[i + 1])));
        }
        return ingredient;
    }

    /** An ingredient where only portions matter (for unit conversion tests). */
    static Ingredient withPortions(String name, String... portions) {
        return ingredient(name, "100", "1", "1", "1", Set.of(), portions);
    }

    static Ingredient tagged(String name, IngredientTag... tags) {
        return ingredient(name, "100", "1", "1", "1", tags.length == 0 ? Set.of() : Set.of(tags));
    }

    static RecipeLine line(Ingredient ingredient, String amount, Unit unit) {
        return new RecipeLine(ingredient, amount == null ? null : new BigDecimal(amount), unit, null, false);
    }

    static RecipeLine optionalLine(Ingredient ingredient, String amount, Unit unit) {
        return new RecipeLine(ingredient, amount == null ? null : new BigDecimal(amount), unit, null, true);
    }
}