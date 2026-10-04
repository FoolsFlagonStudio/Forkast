package com.forkast.backend.ingest;

import static com.forkast.backend.ingest.TestIngredients.line;
import static com.forkast.backend.ingest.TestIngredients.optionalLine;
import static com.forkast.backend.ingest.TestIngredients.tagged;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientTag;

class DietaryLabelClassifierTest {

    private final DietaryLabelClassifier classifier = new DietaryLabelClassifier();

    private final Ingredient pasta = tagged("pasta", IngredientTag.GLUTEN);
    private final Ingredient cheese = tagged("parmesan cheese", IngredientTag.DAIRY);
    private final Ingredient chicken = tagged("chicken breast", IngredientTag.POULTRY);
    private final Ingredient shrimp = tagged("shrimp", IngredientTag.SHELLFISH);
    private final Ingredient garlic = tagged("garlic");

    @Test
    void cheesePastaIsVegetarianButNotVeganOrGlutenFree() {
        Set<String> labels = classifier.classify(List.of(
                line(pasta, "8", Unit.OZ),
                line(cheese, "1", Unit.CUP),
                line(garlic, "2", Unit.CLOVE)), null);

        assertThat(labels).containsExactly(
                "egg-free", "nut-free", "pescatarian", "shellfish-free", "soy-free", "vegetarian");
    }

    @Test
    void poultryRulesOutVegetarianAndPescatarian() {
        Set<String> labels = classifier.classify(List.of(line(chicken, "1", Unit.LB)), null);

        assertThat(labels).doesNotContain("vegetarian", "vegan", "pescatarian").contains("dairy-free", "gluten-free");
    }

    @Test
    void shellfishIsPescatarianButNotShellfishFree() {
        Set<String> labels = classifier.classify(List.of(line(shrimp, "1", Unit.LB)), null);

        assertThat(labels).contains("pescatarian").doesNotContain("vegetarian", "shellfish-free");
    }

    @Test
    void unmatchedRequiredLineBlocksTagLabels() {
        Set<String> labels = classifier.classify(List.of(
                line(garlic, "2", Unit.CLOVE),
                line(null, "1", Unit.CUP)), null);

        assertThat(labels).isEmpty();
    }

    @Test
    void optionalLinesAreIgnored() {
        Set<String> labels = classifier.classify(List.of(
                line(garlic, "2", Unit.CLOVE),
                optionalLine(cheese, "1", Unit.CUP)), null);

        assertThat(labels).contains("vegan", "dairy-free");
    }

    @Test
    void highProteinNeedsCompleteNutritionAndAtLeast25Grams() {
        List<RecipeLine> lines = List.of(line(chicken, "1", Unit.LB));
        NutritionResult complete = new NutritionResult(null, new BigDecimal("30"), null, null, true);
        NutritionResult incomplete = new NutritionResult(null, new BigDecimal("30"), null, null, false);
        NutritionResult low = new NutritionResult(null, new BigDecimal("24.99"), null, null, true);

        assertThat(classifier.classify(lines, complete)).contains("high-protein");
        assertThat(classifier.classify(lines, incomplete)).doesNotContain("high-protein");
        assertThat(classifier.classify(lines, low)).doesNotContain("high-protein");
    }
}