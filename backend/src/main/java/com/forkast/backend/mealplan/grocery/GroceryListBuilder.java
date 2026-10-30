package com.forkast.backend.mealplan.grocery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.forkast.backend.ingest.RecipeLine;
import com.forkast.backend.ingest.Unit;
import com.forkast.backend.ingest.UnitConverter;
import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.pricing.RecipeCostCalculator;
import com.forkast.backend.pricing.ResolvedPrice;

/**
 * Adds up what a week's recipes use and prices it the way a shopper pays: in
 * whole packages.
 * A plain class like RecipeCostCalculator; the service hands it the scaled
 * lines and prices.
 *
 * 1. Lines: required, matched lines with an amount (the ones nutrition and cost
 * count),
 * already scaled by each selection's portion multiplier. Water and ice are left
 * off.
 * 2. Combine: amounts that convert to grams add up into one line per
 * ingredient. Ones that
 * can't ("3 limes" with no weight on file) keep their own unit, so an
 * ingredient can
 * have two lines.
 * 3. Package: of the ingredient's Kroger products, the one that's cheapest for
 * the amount
 * needed, in whole packages. A 3 lb bag can beat a 1 lb bag per gram and still
 * lose to it
 * when the week needs 200 g. By-weight items (loose onions, chicken) cost
 * exactly what's
 * needed. Counted lines use count packages ("12 ct").
 * 4. Usage cost: what the line uses at the ingredient's resolved price, as
 * recipe cost does.
 * Pantry staples count at this, unless the user marks them "need to buy"; lines
 * with no
 * product count at it too, as an estimate.
 *
 * Amounts stay exact; rounding is for display.
 */
public class GroceryListBuilder {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal GRAMS_PER_POUND = new BigDecimal("453.59");

    /**
     * How far past a whole package still counts as that package: a recipe's "10 oz
     * can" reads
     * as 283.64 g against a 283.5 g can, and that's one can, not two.
     */
    static final BigDecimal PACKAGE_SLACK = new BigDecimal("0.03");

    /**
     * One way to buy an ingredient.
     *
     * @param price the package price, or the price per lb when byWeight
     * @param grams what one package weighs, when known
     * @param items items per package for count packages ("12 ct"), else null
     */
    public record PackageOption(String label, BigDecimal price, BigDecimal grams, BigDecimal items, boolean byWeight) {
    }

    /** Where prices come from, so tests can hand in their own. */
    public interface Prices {
        Optional<ResolvedPrice> resolved(Ingredient ingredient);

        List<PackageOption> packages(Ingredient ingredient);
    }

    /**
     * One grocery line. unit is "g" for combined weights, else the recipe unit
     * ("count").
     * usageCost and packageCost are null when they can't be worked out;
     * packageCount is null
     * for by-weight items and lines with no package.
     */
    public record Line(Ingredient ingredient, BigDecimal amount, String unit, boolean pantry,
            BigDecimal usageCost, PackageOption pkg, Integer packageCount, BigDecimal packageCost) {

        /** What the line adds to the grocery cost. */
        public BigDecimal cost(boolean needToBuy) {
            if (pantry && !needToBuy && usageCost != null) {
                return usageCost;
            }
            return packageCost != null ? packageCost : usageCost;
        }

        /** No package to price it by, so the cost is the usage cost or unknown. */
        public boolean estimate() {
            return packageCost == null;
        }
    }

    private record Key(Ingredient ingredient, String unit) {
    }

    private final UnitConverter unitConverter;

    public GroceryListBuilder(UnitConverter unitConverter) {
        this.unitConverter = unitConverter;
    }

    /** Lines in name order, then grams before other units. */
    public List<Line> build(List<RecipeLine> lines, Prices prices) {
        Map<Key, BigDecimal> totals = new LinkedHashMap<>();
        Map<Key, Unit> units = new LinkedHashMap<>();
        for (RecipeLine line : lines) {
            if (!line.countsTowardNutrition() || line.ingredient() == null
                    || RecipeCostCalculator.isFree(line.ingredient())) {
                continue;
            }
            Optional<BigDecimal> grams = unitConverter.toGrams(line.amount(), line.unit(), line.prepNote(),
                    line.ingredient());
            Key key = grams.isPresent()
                    ? new Key(line.ingredient(), "g")
                    : new Key(line.ingredient(), line.unit().name().toLowerCase(Locale.ROOT));
            totals.merge(key, grams.orElse(line.amount()), BigDecimal::add);
            units.put(key, grams.isPresent() ? Unit.G : line.unit());
        }

        List<Line> result = new ArrayList<>();
        totals.forEach((key, amount) -> {
            if (amount.signum() > 0) {
                result.add(price(key.ingredient(), amount, key.unit(), units.get(key), prices));
            }
        });
        result.sort(Comparator.comparing((Line l) -> l.ingredient().getName())
                .thenComparing(l -> !l.unit().equals("g"))
                .thenComparing(Line::unit));
        return result;
    }

    private static Line price(Ingredient ingredient, BigDecimal amount, String unitName, Unit unit, Prices prices) {
        Optional<ResolvedPrice> resolved = prices.resolved(ingredient);
        BigDecimal usageCost = null;
        PackageOption best = null;
        Integer bestCount = null;
        BigDecimal bestCost = null;

        if (unit == Unit.G) {
            if (resolved.isPresent() && resolved.get().per100g() != null) {
                usageCost = money(amount.multiply(resolved.get().per100g()).divide(HUNDRED));
            }
            for (PackageOption option : prices.packages(ingredient)) {
                Integer count;
                BigDecimal cost;
                if (option.byWeight()) {
                    count = null;
                    cost = money(amount.divide(GRAMS_PER_POUND, 6, RoundingMode.HALF_UP).multiply(option.price()));
                } else if (option.grams() != null && option.grams().signum() > 0) {
                    count = packagesFor(amount, option.grams());
                    cost = money(option.price().multiply(BigDecimal.valueOf(count)));
                } else {
                    continue;
                }
                if (better(cost, count, bestCost, bestCount)) {
                    best = option;
                    bestCount = count;
                    bestCost = cost;
                }
            }
        } else if (unit == Unit.COUNT) {
            if (resolved.isPresent() && resolved.get().perItem() != null) {
                usageCost = money(amount.multiply(resolved.get().perItem()));
            }
            for (PackageOption option : prices.packages(ingredient)) {
                if (option.items() == null || option.items().signum() <= 0) {
                    continue;
                }
                int count = packagesFor(amount, option.items());
                BigDecimal cost = money(option.price().multiply(BigDecimal.valueOf(count)));
                if (better(cost, count, bestCost, bestCount)) {
                    best = option;
                    bestCount = count;
                    bestCost = cost;
                }
            }
        }
        return new Line(ingredient, amount, unitName, ingredient.isPantryStaple(), usageCost, best, bestCount,
                bestCost);
    }

    /**
     * Whole packages to cover the amount: 450 g of 400 g cans is 2. Up to 3% over a
     * whole
     * number of packages rounds down, since that's conversion noise, not a real
     * shortfall.
     */
    static int packagesFor(BigDecimal needed, BigDecimal perPackage) {
        BigDecimal exact = needed.divide(perPackage, 6, RoundingMode.HALF_UP);
        BigDecimal whole = exact.setScale(0, RoundingMode.FLOOR);
        BigDecimal count = exact.subtract(whole).compareTo(PACKAGE_SLACK) <= 0 ? whole
                : whole.add(BigDecimal.ONE);
        return count.max(BigDecimal.ONE).intValueExact();
    }

    /** Cheaper wins; on a tie, fewer packages (less to carry, less left over). */
    private static boolean better(BigDecimal cost, Integer count, BigDecimal bestCost, Integer bestCount) {
        if (bestCost == null) {
            return true;
        }
        int byCost = cost.compareTo(bestCost);
        if (byCost != 0) {
            return byCost < 0;
        }
        int packages = count == null ? 0 : count;
        int bestPackages = bestCount == null ? 0 : bestCount;
        return packages < bestPackages;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}