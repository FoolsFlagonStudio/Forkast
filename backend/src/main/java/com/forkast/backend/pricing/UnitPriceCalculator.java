package com.forkast.backend.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.forkast.backend.ingest.Unit;
import com.forkast.backend.ingest.UnitConverter;
import com.forkast.backend.ingredient.Ingredient;

/**
 * Turns "this much money buys this package" into a price per 100 g and, where
 * it makes sense,
 * a price per item.
 *
 * Grams come from UnitConverter, the same conversion recipes use, so a price
 * and a recipe
 * line agree on what "1 cup" or "1 onion" weighs:
 * $3.79 for 3 lb -> $0.2785 per 100 g
 * $5.49 for 16.9 fl oz oil -> per 100 g via the oil's cup portion; none without
 * one
 * $3.49 for 12 ct eggs -> $0.2908 per item, and per 100 g via the "large"
 * portion
 *
 * A store sells loose produce by weight ($1.29 per lb) but a recipe says "1
 * onion". When the
 * store gives an average weight per item (Kroger's averageWeightPerUnit), the
 * caller passes it
 * in and the price per item comes from that. The caller decides when it
 * applies: an average
 * weight for a tray of chicken is the tray, not one breast, so it's only passed
 * for produce.
 */
@Component
public class UnitPriceCalculator {

    static final BigDecimal GRAMS_PER_LB = new BigDecimal("453.59");

    private final UnitConverter unitConverter;

    public UnitPriceCalculator(UnitConverter unitConverter) {
        this.unitConverter = unitConverter;
    }

    /**
     * @param price               what the package costs (for a by-weight item, the
     *                            price per
     *                            pound with a size of "1 lb")
     * @param size                what the package holds
     * @param averageGramsPerItem one item's typical weight, or null when not
     *                            applicable
     */
    public UnitPrice calculate(BigDecimal price, PackageSize size, BigDecimal averageGramsPerItem,
            Ingredient ingredient) {
        if (price == null || price.signum() <= 0 || size == null) {
            return UnitPrice.NONE;
        }

        Optional<BigDecimal> grams = unitConverter.toGrams(size.quantity(), size.unit(), null, ingredient)
                .filter(g -> g.signum() > 0);
        BigDecimal per100g = grams
                .map(g -> price.multiply(BigDecimal.valueOf(100)).divide(g, 4, RoundingMode.HALF_UP))
                .orElse(null);

        BigDecimal perItem = null;
        if (size.unit() == Unit.COUNT) {
            perItem = price.divide(size.quantity(), 4, RoundingMode.HALF_UP);
        } else if (averageGramsPerItem != null && averageGramsPerItem.signum() > 0 && per100g != null) {
            perItem = per100g.multiply(averageGramsPerItem)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
        return new UnitPrice(per100g, perItem);
    }

    /**
     * A package counted rather than weighed ("1 ct" celery, "12 ct" eggs) when the
     * store says
     * what the package weighs: per 100 g from that weight, per item from the count.
     * Without a
     * weight there's no price per 100 g at all, because guessing "one" from the
     * catalog's
     * portions can be off by twenty times (a bunch of celery is not a celery
     * strip).
     */
    public static UnitPrice forCountPackage(BigDecimal price, BigDecimal count, BigDecimal packageGrams) {
        if (price == null || price.signum() <= 0 || count == null || count.signum() <= 0) {
            return UnitPrice.NONE;
        }
        BigDecimal perItem = price.divide(count, 4, RoundingMode.HALF_UP);
        BigDecimal per100g = packageGrams != null && packageGrams.signum() > 0
                ? price.multiply(BigDecimal.valueOf(100)).divide(packageGrams, 4, RoundingMode.HALF_UP)
                : null;
        return new UnitPrice(per100g, perItem);
    }

    /** Kroger gives average weights in pounds ("0.5 [lb_av]"). */
    public static BigDecimal poundsToGrams(BigDecimal pounds) {
        return pounds == null ? null : pounds.multiply(GRAMS_PER_LB).setScale(2, RoundingMode.HALF_UP);
    }
}