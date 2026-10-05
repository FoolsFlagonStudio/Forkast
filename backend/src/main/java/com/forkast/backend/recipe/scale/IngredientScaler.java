package com.forkast.backend.recipe.scale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.forkast.backend.ingest.Unit;

/**
 * Scales ingredient amounts to a different number of servings and formats them
 * the way a
 * cook reads them. Plain static methods, no database, so it's unit-tested
 * directly.
 */
public final class IngredientScaler {

    /** Units people measure in fractions: "1/3 cup", "2/3 can". */
    private static final Set<Unit> FRACTION_UNITS = EnumSet.of(
            Unit.TSP, Unit.TBSP, Unit.CUP, Unit.FL_OZ, Unit.PINT, Unit.QUART, Unit.GALLON,
            Unit.COUNT, Unit.CLOVE, Unit.SLICE, Unit.CAN, Unit.PINCH);

    /** Units shown as whole numbers: nobody weighs 151.33 g. */
    private static final Set<Unit> WHOLE_UNITS = EnumSet.of(Unit.G, Unit.ML);

    private record Fraction(BigDecimal value, String label) {
    }

    /**
     * The fractions measuring cups and spoons come in, plus 0 and 1 for rounding to
     * a whole.
     */
    private static final List<Fraction> FRACTIONS = List.of(
            new Fraction(new BigDecimal("0"), ""),
            new Fraction(new BigDecimal("0.125"), "1/8"),
            new Fraction(new BigDecimal("0.25"), "1/4"),
            new Fraction(new BigDecimal("0.3333"), "1/3"),
            new Fraction(new BigDecimal("0.5"), "1/2"),
            new Fraction(new BigDecimal("0.6667"), "2/3"),
            new Fraction(new BigDecimal("0.75"), "3/4"),
            new Fraction(new BigDecimal("1"), ""));

    private IngredientScaler() {
    }

    /**
     * servings / baseServings, kept to 6 decimals so 2/6 stays close to a third.
     */
    public static BigDecimal factor(int servings, int baseServings) {
        if (servings < 1 || baseServings < 1) {
            throw new IllegalArgumentException("servings and baseServings must be at least 1");
        }
        return BigDecimal.valueOf(servings).divide(BigDecimal.valueOf(baseServings), 6, RoundingMode.HALF_UP);
    }

    private static final BigDecimal QUARTER = new BigDecimal("0.25");
    private static final BigDecimal TBSP_PER_CUP = new BigDecimal("16");
    private static final BigDecimal TSP_PER_TBSP = new BigDecimal("3");

    /** unit is the stored lowercase enum name ("cup", "fl_oz", "to_taste"). */
    public static ScaledAmount scale(BigDecimal amount, String unit, BigDecimal factor) {
        if (amount == null) {
            return new ScaledAmount(null, unit, null);
        }
        BigDecimal scaled = amount.multiply(factor);
        Unit parsed = toUnit(unit);

        // Nobody measures 1/12 cup: step down to tablespoons, then teaspoons.
        if (parsed == Unit.CUP && scaled.compareTo(QUARTER) < 0) {
            scaled = scaled.multiply(TBSP_PER_CUP);
            parsed = Unit.TBSP;
        }
        if (parsed == Unit.TBSP && scaled.compareTo(BigDecimal.ONE) < 0) {
            scaled = scaled.multiply(TSP_PER_TBSP);
            parsed = Unit.TSP;
        }

        scaled = scaled.setScale(3, RoundingMode.HALF_UP);
        String unitCode = parsed != null ? parsed.name().toLowerCase(Locale.ROOT) : unit;
        return new ScaledAmount(scaled, unitCode, display(scaled, parsed));
    }

    // ---------- display ----------

    static String display(BigDecimal amount, Unit unit) {
        if (unit != null && FRACTION_UNITS.contains(unit)) {
            return asFraction(amount);
        }
        if (unit != null && WHOLE_UNITS.contains(unit)) {
            BigDecimal whole = amount.setScale(0, RoundingMode.HALF_UP);
            return whole.signum() == 0 ? "1" : whole.toPlainString(); // never show "0 g"
        }
        // oz, lb, kg, l, or anything unrecognized: up to 2 decimals
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        if (rounded.signum() == 0) {
            return "0.01";
        }
        return rounded.stripTrailingZeros().toPlainString();
    }

    /** Whole part plus the nearest common fraction: 1.4 -> "1 1/3", 0.95 -> "1". */
    private static String asFraction(BigDecimal amount) {
        BigDecimal whole = amount.setScale(0, RoundingMode.FLOOR);
        BigDecimal remainder = amount.subtract(whole);

        Fraction nearest = FRACTIONS.get(0);
        for (Fraction fraction : FRACTIONS) {
            if (remainder.subtract(fraction.value()).abs()
                    .compareTo(remainder.subtract(nearest.value()).abs()) < 0) {
                nearest = fraction;
            }
        }
        if (nearest.value().compareTo(BigDecimal.ONE) == 0) {
            whole = whole.add(BigDecimal.ONE); // 0.95 rounds up to the next whole
        }

        boolean hasWhole = whole.signum() > 0;
        boolean hasFraction = !nearest.label().isEmpty();
        if (hasWhole && hasFraction) {
            return whole.toPlainString() + " " + nearest.label();
        }
        if (hasWhole) {
            return whole.toPlainString();
        }
        return hasFraction ? nearest.label() : "1/8"; // too small to show: the smallest measure
    }

    private static Unit toUnit(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            return Unit.valueOf(stored.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}