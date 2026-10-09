package com.forkast.backend.pricing.kroger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.forkast.backend.ingredient.SoldBy;
import com.forkast.backend.pricing.UnitPriceCalculator;

/**
 * One product's price at the store, read out of Kroger's response and ready to
 * store.
 *
 * averageGramsPerItem is only set for produce sold by weight (a loose onion
 * averages 0.5 lb),
 * so a price per item means one onion. For meat sold by weight it would be a
 * whole tray, so
 * it's left out.
 *
 * packageGrams is what one package weighs, for items sold by the package and
 * counted rather
 * than weighed ("1 ct" celery is one bunch). Without it, "one" would be
 * converted with the
 * catalog's smallest portion (a celery strip), pricing a bunch as if it weighed
 * 4 g.
 */
public record KrogerQuote(
        String productId,
        String label,
        BigDecimal regular,
        BigDecimal promo,
        String size,
        SoldBy soldBy,
        BigDecimal averageGramsPerItem,
        BigDecimal packageGrams) {

    private static final Pattern WEIGHT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*\\[?\\s*(lb|oz)?");
    private static final BigDecimal GRAMS_PER_OZ = new BigDecimal("28.35");

    /**
     * Empty when the store has no price for it (not stocked, or no item with a
     * price).
     */
    public static Optional<KrogerQuote> from(KrogerProduct product) {
        if (product == null || product.items() == null) {
            return Optional.empty();
        }
        return product.items().stream()
                .filter(item -> item.price() != null && item.price().regular() != null
                        && item.price().regular().signum() > 0)
                .findFirst()
                .map(item -> {
                    SoldBy soldBy = parseSoldBy(item.soldBy());
                    BigDecimal promo = item.price().promo() != null && item.price().promo().signum() > 0
                            ? item.price().promo()
                            : null;
                    KrogerProduct.ItemInformation info = product.itemInformation();
                    BigDecimal average = soldBy == SoldBy.WEIGHT && isProduce(product.categories()) && info != null
                            ? grams(info.averageWeightPerUnit())
                            : null;
                    BigDecimal packageGrams = soldBy != SoldBy.WEIGHT && info != null
                            ? firstNonNull(grams(info.averageWeightPerUnit()), grams(info.netWeight()))
                            : null;
                    return new KrogerQuote(product.productId(), cleanLabel(product.description()),
                            item.price().regular(), promo, item.size(), soldBy, average, packageGrams);
                });
    }

    /**
     * Kroger sends "®" as mangled bytes ("Simple TruthÂ®"); drop the marks for a
     * readable label.
     */
    static String cleanLabel(String description) {
        if (description == null) {
            return null;
        }
        return description.replace("Â", "").replace("®", "").replace("™", "").replaceAll("\\s+", " ").trim();
    }

    private static SoldBy parseSoldBy(String soldBy) {
        if (soldBy == null) {
            return null;
        }
        try {
            return SoldBy.valueOf(soldBy.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isProduce(List<String> categories) {
        return categories != null && categories.stream().anyMatch(c -> c.equalsIgnoreCase("Produce"));
    }

    /**
     * "0.5 [lb_av]" -> 226.80; "12 [oz_av]" -> 340.20; pounds when no unit is
     * given.
     */
    static BigDecimal grams(String weight) {
        if (weight == null) {
            return null;
        }
        Matcher m = WEIGHT.matcher(weight.toLowerCase());
        if (!m.find()) {
            return null;
        }
        BigDecimal amount = new BigDecimal(m.group(1));
        if (amount.signum() <= 0) {
            return null;
        }
        BigDecimal grams = "oz".equals(m.group(2))
                ? amount.multiply(GRAMS_PER_OZ)
                : UnitPriceCalculator.poundsToGrams(amount);
        return grams.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal firstNonNull(BigDecimal a, BigDecimal b) {
        return a != null ? a : b;
    }
}