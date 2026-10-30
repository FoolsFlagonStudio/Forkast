package com.forkast.backend.mealplan.grocery;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.forkast.backend.ingest.RecipeLine;
import com.forkast.backend.ingest.Unit;
import com.forkast.backend.ingest.UnitConverter;
import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientPortion;
import com.forkast.backend.ingredient.PriceSource;
import com.forkast.backend.mealplan.grocery.GroceryListBuilder.Line;
import com.forkast.backend.mealplan.grocery.GroceryListBuilder.PackageOption;
import com.forkast.backend.pricing.ResolvedPrice;

class GroceryListBuilderTest {

    private final GroceryListBuilder builder = new GroceryListBuilder(new UnitConverter());

    private final Ingredient chicken = new Ingredient("chicken breast");
    private final Ingredient beans = new Ingredient("black beans");
    private final Ingredient onion = withPortion("onion", "medium", "110");
    private final Ingredient lime = new Ingredient("lime"); // no weight on file
    private final Ingredient oil = pantry(withPortion("olive oil", "tbsp", "13.5"));
    private final Ingredient water = new Ingredient("water");
    private final Ingredient saffron = new Ingredient("saffron");

    private final Map<Ingredient, ResolvedPrice> resolved = new HashMap<>();
    private final Map<Ingredient, List<PackageOption>> packages = new HashMap<>();

    private final GroceryListBuilder.Prices prices = new GroceryListBuilder.Prices() {
        @Override
        public Optional<ResolvedPrice> resolved(Ingredient ingredient) {
            return Optional.ofNullable(resolved.get(ingredient));
        }

        @Override
        public List<PackageOption> packages(Ingredient ingredient) {
            return packages.getOrDefault(ingredient, List.of());
        }
    };

    // ---------- helpers ----------

    private static Ingredient withPortion(String name, String portion, String grams) {
        Ingredient ingredient = new Ingredient(name);
        ingredient.addPortion(new IngredientPortion(portion, new BigDecimal(grams)));
        return ingredient;
    }

    private static Ingredient pantry(Ingredient ingredient) {
        ingredient.setPantryStaple(true);
        return ingredient;
    }

    private static RecipeLine line(Ingredient ingredient, String amount, Unit unit) {
        return new RecipeLine(ingredient, amount == null ? null : new BigDecimal(amount), unit, null, false);
    }

    private static ResolvedPrice price(String per100g, String perItem) {
        return new ResolvedPrice(per100g == null ? null : new BigDecimal(per100g),
                perItem == null ? null : new BigDecimal(perItem), PriceSource.KROGER, Instant.now(), false);
    }

    private static PackageOption pack(String label, String price, String grams) {
        return new PackageOption(label, new BigDecimal(price), new BigDecimal(grams), null, false);
    }

    private Line only(List<RecipeLine> lines) {
        List<Line> built = builder.build(lines, prices);
        assertThat(built).hasSize(1);
        return built.get(0);
    }

    // ---------- combining ----------

    @Test
    void weightsOfOneIngredientAddUpIntoOneLine() {
        Line line = only(List.of(line(chicken, "200", Unit.G), line(chicken, "1", Unit.LB)));

        assertThat(line.unit()).isEqualTo("g");
        assertThat(line.amount()).isEqualByComparingTo("653.59");
    }

    @Test
    void countsWithNoWeightKeepTheirOwnLine() {
        List<Line> built = builder.build(List.of(
                line(lime, "3", Unit.COUNT), line(lime, "2", Unit.COUNT), line(lime, "30", Unit.G)), prices);

        assertThat(built).extracting(Line::unit).containsExactly("g", "count");
        assertThat(built.get(1).amount()).isEqualByComparingTo("5");
    }

    @Test
    void countsWithAWeightOnFileBecomeGrams() {
        Line line = only(List.of(line(onion, "2", Unit.COUNT)));

        assertThat(line.unit()).isEqualTo("g");
        assertThat(line.amount()).isEqualByComparingTo("220");
    }

    @Test
    void optionalPinchToTasteUnmatchedAndWaterAreLeftOff() {
        List<RecipeLine> lines = List.of(
                new RecipeLine(chicken, new BigDecimal("100"), Unit.G, null, true),
                line(chicken, "1", Unit.PINCH),
                line(chicken, null, Unit.TO_TASTE),
                line(null, "2", Unit.CUP),
                line(water, "4", Unit.CUP));

        assertThat(builder.build(lines, prices)).isEmpty();
    }

    // ---------- packages ----------

    @Test
    void packagesRoundUp() {
        packages.put(beans, List.of(pack("Black Beans, 15 oz", "1.00", "425")));

        Line line = only(List.of(line(beans, "450", Unit.G)));

        assertThat(line.packageCount()).isEqualTo(2);
        assertThat(line.packageCost()).isEqualByComparingTo("2.00");
        assertThat(line.estimate()).isFalse();
    }

    @Test
    void theCheapestPackageForTheAmountWinsNotTheCheapestPerGram() {
        packages.put(chicken, List.of(
                pack("1 lb", "2.00", "453.59"),
                pack("3 lb", "4.50", "1360.77")));

        Line small = only(List.of(line(chicken, "200", Unit.G)));
        Line large = only(List.of(line(chicken, "1200", Unit.G)));

        assertThat(small.pkg().label()).isEqualTo("1 lb");
        assertThat(small.packageCost()).isEqualByComparingTo("2.00");
        assertThat(large.pkg().label()).isEqualTo("3 lb"); // beats three 1 lb packs at 6.00
        assertThat(large.packageCount()).isEqualTo(1);
        assertThat(large.packageCost()).isEqualByComparingTo("4.50");
    }

    @Test
    void byWeightItemsCostExactlyWhatsNeeded() {
        packages.put(chicken,
                List.of(new PackageOption("Chicken (by the lb)", new BigDecimal("3.00"), null, null, true)));

        Line line = only(List.of(line(chicken, "1.5", Unit.LB)));

        assertThat(line.packageCount()).isNull();
        assertThat(line.packageCost()).isEqualByComparingTo("4.50");
    }

    @Test
    void countedLinesUseCountPackages() {
        packages.put(lime,
                List.of(new PackageOption("Limes, 3 ct", new BigDecimal("1.50"), null, new BigDecimal("3"), false)));

        Line line = only(List.of(line(lime, "5", Unit.COUNT)));

        assertThat(line.packageCount()).isEqualTo(2);
        assertThat(line.packageCost()).isEqualByComparingTo("3.00");
    }

    // ---------- what a line costs ----------

    @Test
    void pantryStaplesCountAtUsageCostUnlessNeeded() {
        resolved.put(oil, price("1.00", null));
        packages.put(oil, List.of(pack("Olive Oil, 16.9 fl oz", "8.00", "460")));

        Line line = only(List.of(line(oil, "2", Unit.TBSP))); // 27 g

        assertThat(line.pantry()).isTrue();
        assertThat(line.usageCost()).isEqualByComparingTo("0.27");
        assertThat(line.cost(false)).isEqualByComparingTo("0.27");
        assertThat(line.cost(true)).isEqualByComparingTo("8.00");
    }

    @Test
    void withNoPackageTheUsageCostIsAnEstimate() {
        resolved.put(beans, price("0.40", null));

        Line line = only(List.of(line(beans, "500", Unit.G)));

        assertThat(line.estimate()).isTrue();
        assertThat(line.cost(false)).isEqualByComparingTo("2.00");
    }

    @Test
    void withNoPriceAtAllTheCostIsUnknown() {
        Line line = only(List.of(line(saffron, "1", Unit.G)));

        assertThat(line.cost(false)).isNull();
        assertThat(line.estimate()).isTrue();
    }

    @Test
    void anExactMultipleNeedsNoExtraPackage() {
        assertThat(GroceryListBuilder.packagesFor(new BigDecimal("800"), new BigDecimal("400"))).isEqualTo(2);
        assertThat(GroceryListBuilder.packagesFor(new BigDecimal("0.5"), new BigDecimal("400"))).isEqualTo(1);
    }

    @Test
    void conversionNoiseJustOverAPackageIsStillOnePackage() {
        // "1 (10 oz) can" reads as 283.64 g; the can is 283.5 g
        assertThat(GroceryListBuilder.packagesFor(new BigDecimal("283.64"), new BigDecimal("283.5"))).isEqualTo(1);
        assertThat(GroceryListBuilder.packagesFor(new BigDecimal("300"), new BigDecimal("283.5"))).isEqualTo(2);
    }
}