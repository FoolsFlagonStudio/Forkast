package com.forkast.backend.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.forkast.backend.ingest.Unit;
import com.forkast.backend.ingest.UnitConverter;
import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientPortion;

/**
 * The prices are the real ones from the Kroger test run where there was one.
 */
class UnitPriceCalculatorTest {

    private final UnitPriceCalculator calculator = new UnitPriceCalculator(new UnitConverter());

    private static Ingredient ingredient(String name, String... portions) {
        Ingredient ingredient = new Ingredient(name);
        for (int i = 0; i < portions.length; i += 2) {
            ingredient.addPortion(new IngredientPortion(portions[i], new BigDecimal(portions[i + 1])));
        }
        return ingredient;
    }

    private UnitPrice calc(String price, String quantity, Unit unit, String avgGrams, Ingredient ingredient) {
        return calculator.calculate(new BigDecimal(price), new PackageSize(new BigDecimal(quantity), unit),
                avgGrams == null ? null : new BigDecimal(avgGrams), ingredient);
    }

    @Test
    void packageByWeight() {
        // Kroger Yellow Onion 3 lb Bag, $3.79, sold by UNIT
        UnitPrice bag = calc("3.79", "3", Unit.LB, null, ingredient("onion"));

        assertThat(bag.per100g()).isEqualByComparingTo("0.2785"); // 3.79 / 1360.77 g * 100
        assertThat(bag.perItem()).isNull();
    }

    @Test
    void looseProduceGetsAPricePerItemFromItsAverageWeight() {
        // Onions - Yellow, $1.29 per lb, averageWeightPerUnit 0.5 lb
        BigDecimal avg = UnitPriceCalculator.poundsToGrams(new BigDecimal("0.5"));
        UnitPrice loose = calc("1.29", "1", Unit.LB, avg.toPlainString(), ingredient("onion"));

        assertThat(avg).isEqualByComparingTo("226.80");
        assertThat(loose.per100g()).isEqualByComparingTo("0.2844");
        assertThat(loose.perItem()).isEqualByComparingTo("0.6450"); // about half of $1.29
    }

    @Test
    void meatByThePoundHasNoPricePerItem() {
        // Perdue boneless skinless chicken breasts, $4.99 per lb
        UnitPrice chicken = calc("4.99", "1", Unit.LB, null, ingredient("chicken breast"));

        assertThat(chicken.per100g()).isEqualByComparingTo("1.1001");
        assertThat(chicken.perItem()).isNull();
    }

    @Test
    void volumeUsesTheIngredientsDensity() {
        // 1 cup of olive oil weighs 216 g, so 16.9 fl oz (500 ml) is about 456 g
        UnitPrice oil = calc("5.49", "16.9", Unit.FL_OZ, null, ingredient("olive oil", "cup", "216"));

        assertThat(oil.per100g()).isEqualByComparingTo("1.2031");
        assertThat(oil.isKnown()).isTrue();
    }

    @Test
    void volumeWithoutADensityIsUnknown() {
        UnitPrice oil = calc("5.49", "16.9", Unit.FL_OZ, null, ingredient("mystery oil"));

        assertThat(oil).isEqualTo(UnitPrice.NONE);
        assertThat(oil.isKnown()).isFalse();
    }

    @Test
    void countsGiveAPricePerItemAndPer100gWhenThereIsASize() {
        // a dozen large eggs at $3.49; one large egg is 50 g
        UnitPrice eggs = calc("3.49", "12", Unit.COUNT, null, ingredient("egg", "large", "50"));

        assertThat(eggs.perItem()).isEqualByComparingTo("0.2908");
        assertThat(eggs.per100g()).isEqualByComparingTo("0.5817"); // 3.49 / 600 g * 100
    }

    @Test
    void countsWithoutAnItemWeightStillPricePerItem() {
        UnitPrice limes = calc("2.00", "4", Unit.COUNT, null, ingredient("lime"));

        assertThat(limes.perItem()).isEqualByComparingTo("0.50");
        assertThat(limes.per100g()).isNull();
    }

    @Test
    void countedPackageUsesItsWeightNotACatalogPortion() {
        // Kroger "1 ct" celery at $2.59: one bunch weighing 1.5 lb (680.39 g)
        UnitPrice bunch = UnitPriceCalculator.forCountPackage(new BigDecimal("2.59"), BigDecimal.ONE,
                new BigDecimal("680.39"));

        assertThat(bunch.per100g()).isEqualByComparingTo("0.3807");
        assertThat(bunch.perItem()).isEqualByComparingTo("2.59");
    }

    @Test
    void countedPackageWithoutAWeightHasOnlyAPricePerItem() {
        UnitPrice eggs = UnitPriceCalculator.forCountPackage(new BigDecimal("3.49"), new BigDecimal("12"), null);

        assertThat(eggs.per100g()).isNull();
        assertThat(eggs.perItem()).isEqualByComparingTo("0.2908");
        assertThat(UnitPriceCalculator.forCountPackage(BigDecimal.ZERO, BigDecimal.ONE, null))
                .isEqualTo(UnitPrice.NONE);
    }

    @Test
    void missingOrZeroPriceIsUnknown() {
        Ingredient onion = ingredient("onion");
        PackageSize lb = new PackageSize(BigDecimal.ONE, Unit.LB);

        assertThat(calculator.calculate(null, lb, null, onion)).isEqualTo(UnitPrice.NONE);
        assertThat(calculator.calculate(BigDecimal.ZERO, lb, null, onion)).isEqualTo(UnitPrice.NONE);
        assertThat(calculator.calculate(BigDecimal.ONE, null, null, onion)).isEqualTo(UnitPrice.NONE);
    }
}