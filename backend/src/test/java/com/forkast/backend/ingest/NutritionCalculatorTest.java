package com.forkast.backend.ingest;

import static com.forkast.backend.ingest.TestIngredients.ingredient;
import static com.forkast.backend.ingest.TestIngredients.line;
import static com.forkast.backend.ingest.TestIngredients.optionalLine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.forkast.backend.ingredient.Ingredient;

class NutritionCalculatorTest {

    private final NutritionCalculator calculator = new NutritionCalculator(new UnitConverter());

    private final Ingredient chicken = ingredient("chicken breast", "120", "22.5", "0", "2.62", Set.of());
    private final Ingredient flour = ingredient("flour", "364", "10.33", "76.31", "0.98", Set.of(), "cup", "125");
    private final Ingredient salt = ingredient("salt", "0", "0", "0", "0", Set.of(), "tsp", "6");
    private final Ingredient cheese = ingredient("parmesan cheese", "431", "38.46", "4.06", "28.61", Set.of(), "cup",
            "100");

    @Test
    void sumsLinesAndDividesByServings() {
        // chicken 500 g: 600 kcal, 112.5 P, 0 C, 13.1 F
        // flour 100 g: 364 kcal, 10.33 P, 76.31 C, 0.98 F
        NutritionResult result = calculator.calculate(List.of(
                line(chicken, "500", Unit.G),
                line(flour, "100", Unit.G)), 2);

        assertThat(result.caloriesPerServing()).isEqualByComparingTo("482.00");
        assertThat(result.proteinGPerServing()).isEqualByComparingTo("61.42");
        assertThat(result.carbsGPerServing()).isEqualByComparingTo("38.16");
        assertThat(result.fatGPerServing()).isEqualByComparingTo("7.04");
        assertThat(result.complete()).isTrue();
    }

    @Test
    void optionalAndToTasteLinesAreLeftOut() {
        NutritionResult result = calculator.calculate(List.of(
                line(chicken, "100", Unit.G),
                optionalLine(cheese, "1", Unit.CUP),
                line(salt, null, Unit.TO_TASTE)), 1);

        assertThat(result.caloriesPerServing()).isEqualByComparingTo("120");
        assertThat(result.complete()).isTrue();
    }

    @Test
    void unmatchedLineMakesItIncompleteButKeepsTheRest() {
        NutritionResult result = calculator.calculate(List.of(
                line(chicken, "100", Unit.G),
                line(null, "1", Unit.CUP)), 1);

        assertThat(result.caloriesPerServing()).isEqualByComparingTo("120");
        assertThat(result.complete()).isFalse();
    }

    @Test
    void unitWithNoMatchingPortionMakesItIncomplete() {
        // chicken has no volume portion, so "1 cup chicken" can't become grams
        NutritionResult result = calculator.calculate(List.of(line(chicken, "1", Unit.CUP)), 1);

        assertThat(result.complete()).isFalse();
    }

    @Test
    void ingredientWithoutNutrientsMakesItIncomplete() {
        Ingredient mystery = ingredient("mystery", null, null, null, null, Set.of());

        NutritionResult result = calculator.calculate(List.of(line(mystery, "50", Unit.G)), 1);

        assertThat(result.complete()).isFalse();
    }

    @Test
    void servingsMustBePositive() {
        assertThatThrownBy(() -> calculator.calculate(List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}