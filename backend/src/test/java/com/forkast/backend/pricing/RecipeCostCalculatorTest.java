package com.forkast.backend.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.forkast.backend.ingest.RecipeLine;
import com.forkast.backend.ingest.Unit;
import com.forkast.backend.ingest.UnitConverter;
import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientPortion;
import com.forkast.backend.ingredient.PriceSource;

/**
 * Hand-built ingredients and prices; ingredients are looked up by name, no
 * database.
 */
class RecipeCostCalculatorTest {

    private final RecipeCostCalculator calculator = new RecipeCostCalculator(new UnitConverter());

    private final Ingredient chicken = ingredient("chicken breast");
    private final Ingredient onion = ingredient("onion", "medium", "110");
    private final Ingredient lime = ingredient("lime");
    private final Ingredient oil = ingredient("olive oil", "tbsp", "13.5");
    private final Ingredient salt = ingredient("salt", "tsp", "6");
    private final Ingredient water = ingredient("water", "cup", "237");
    private final Ingredient saffron = ingredient("saffron");

    /** per 100 g, per item */
    private final Map<String, ResolvedPrice> prices = Map.of(
            "chicken breast", price("1.1001", null),
            "onion", price("0.2785", "0.6450"),
            "lime", price(null, "0.50"),
            "olive oil", price("1.2031", null),
            "salt", price("0.0500", null));

    private final Function<Ingredient, Optional<ResolvedPrice>> priceOf = i -> Optional
            .ofNullable(prices.get(i.getName()));

    private static Ingredient ingredient(String name, String... portions) {
        Ingredient ingredient = new Ingredient(name);
        for (int i = 0; i < portions.length; i += 2) {
            ingredient.addPortion(new IngredientPortion(portions[i], new BigDecimal(portions[i + 1])));
        }
        return ingredient;
    }

    private static ResolvedPrice price(String per100g, String perItem) {
        return new ResolvedPrice(per100g == null ? null : new BigDecimal(per100g),
                perItem == null ? null : new BigDecimal(perItem), PriceSource.KROGER, Instant.EPOCH, false);
    }

    private static RecipeLine line(Ingredient ingredient, String amount, Unit unit) {
        return new RecipeLine(ingredient, amount == null ? null : new BigDecimal(amount), unit, null, false);
    }

    private static RecipeLine optional(Ingredient ingredient, String amount, Unit unit) {
        return new RecipeLine(ingredient, new BigDecimal(amount), unit, null, true);
    }

    @Test
    void sumsLinesAndDividesByServings() {
        // chicken 2 lb = 907.18 g -> $9.98; 2 tbsp oil = 27 g -> $0.32; total $10.30 /
        // 4
        RecipeCost cost = calculator.calculate(List.of(
                line(chicken, "2", Unit.LB),
                line(oil, "2", Unit.TBSP)), 4, priceOf);

        assertThat(cost.costPerServing()).isEqualByComparingTo("2.58");
        assertThat(cost.complete()).isTrue();
    }

    @Test
    void countLineUsesGramsWhenItCanThenPricePerItem() {
        // 2 onions = 220 g at the bag price: $0.61, not 2 x $0.645
        RecipeCost onions = calculator.calculate(List.of(line(onion, "2", Unit.COUNT)), 1, priceOf);
        // a lime has no portion to weigh, so the price per item is used
        RecipeCost limes = calculator.calculate(List.of(line(lime, "3", Unit.COUNT)), 1, priceOf);

        assertThat(onions.costPerServing()).isEqualByComparingTo("0.61");
        assertThat(limes.costPerServing()).isEqualByComparingTo("1.50");
        assertThat(limes.complete()).isTrue();
    }

    @Test
    void optionalPinchAndToTasteCostNothing() {
        RecipeCost cost = calculator.calculate(List.of(
                line(chicken, "1", Unit.LB),
                optional(oil, "1", Unit.TBSP),
                line(salt, "1", Unit.PINCH),
                line(salt, null, Unit.TO_TASTE)), 1, priceOf);

        assertThat(cost.costPerServing()).isEqualByComparingTo("4.99"); // the chicken alone
        assertThat(cost.complete()).isTrue();
    }

    @Test
    void waterIsFree() {
        RecipeCost cost = calculator.calculate(List.of(
                line(chicken, "1", Unit.LB),
                line(water, "4", Unit.CUP)), 1, priceOf);

        assertThat(cost.costPerServing()).isEqualByComparingTo("4.99");
        assertThat(cost.complete()).isTrue();
    }

    @Test
    void waterAloneIsUnknownNotFree() {
        RecipeCost cost = calculator.calculate(List.of(
                line(water, "4", Unit.CUP),
                line(saffron, "1", Unit.G)), 1, priceOf);

        assertThat(cost.costPerServing()).isNull();
        assertThat(cost.complete()).isFalse();
    }

    @Test
    void unpricedOrUnmatchedLinesMakeItIncompleteButKeepTheRest() {
        RecipeCost cost = calculator.calculate(List.of(
                line(chicken, "1", Unit.LB),
                line(saffron, "1", Unit.G), // no price
                line(null, "1", Unit.CUP)), 1, priceOf); // unmatched

        assertThat(cost.costPerServing()).isEqualByComparingTo("4.99");
        assertThat(cost.complete()).isFalse();
    }

    @Test
    void amountThatCantBeConvertedIsIncomplete() {
        // a cup of chicken: no cup portion, and a price per item doesn't apply to cups
        RecipeCost cost = calculator.calculate(List.of(line(chicken, "1", Unit.CUP)), 1, priceOf);

        assertThat(cost.costPerServing()).isNull();
        assertThat(cost.complete()).isFalse();
    }

    @Test
    void nothingPricedIsNullNotZero() {
        RecipeCost cost = calculator.calculate(List.of(line(saffron, "1", Unit.G)), 2, priceOf);

        assertThat(cost.costPerServing()).isNull();
        assertThat(cost.complete()).isFalse();
    }

    @Test
    void servingsMustBePositive() {
        assertThatThrownBy(() -> calculator.calculate(List.of(), 0, priceOf))
                .isInstanceOf(IllegalArgumentException.class);
    }
}