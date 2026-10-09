package com.forkast.backend.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.forkast.backend.ingest.Unit;

/**
 * Reads a store's package size text into a quantity and a unit: "3 lb", "16.9
 * fl oz", "30 fo",
 * "1/2 gal", "12 ct", "12 x 12 fl oz", "about 2.5 lb". Returns empty when the
 * text has no
 * amount in a unit we can price ("family size", "1 can"), and the price is then
 * stored without
 * a unit price.
 */
public final class PackageSizeParser {

    private static final String NUMBER = "(\\d+(?:\\.\\d+)?|\\d+/\\d+|\\.\\d+)";
    private static final String UNIT_WORD = "(fl\\.?\\s*oz\\.?|[a-z]+\\.?)";

    /** "12 x 12 fl oz", "6 x 1.5 oz": a multipack, priced as its total */
    private static final Pattern MULTIPACK = Pattern.compile(
            "(\\d+)\\s*(?:x|×)\\s*" + NUMBER + "\\s*" + UNIT_WORD);

    private static final Pattern AMOUNT = Pattern.compile(NUMBER + "\\s*" + UNIT_WORD);

    /** Words that mean "items". A dozen is twelve of them. */
    private static final Map<String, BigDecimal> COUNT_WORDS = Map.of(
            "ct", BigDecimal.ONE,
            "count", BigDecimal.ONE,
            "each", BigDecimal.ONE,
            "ea", BigDecimal.ONE,
            "pk", BigDecimal.ONE,
            "pack", BigDecimal.ONE,
            "piece", BigDecimal.ONE,
            "pieces", BigDecimal.ONE,
            "dozen", new BigDecimal("12"),
            "dz", new BigDecimal("12"));

    /** Only weights, volumes and counts can become a unit price. */
    private static final Set<Unit> PRICEABLE = EnumSet.of(
            Unit.OZ, Unit.LB, Unit.G, Unit.KG,
            Unit.FL_OZ, Unit.PINT, Unit.QUART, Unit.GALLON, Unit.ML, Unit.L,
            Unit.COUNT);

    private PackageSizeParser() {
    }

    public static Optional<PackageSize> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String cleaned = text.toLowerCase(Locale.ROOT)
                .replaceAll("\\b(about|approx\\.?|approximately|avg\\.?|average)\\b", " ")
                .replace("~", " ")
                .trim();

        Matcher multipack = MULTIPACK.matcher(cleaned);
        if (multipack.find()) {
            Optional<PackageSize> each = toSize(multipack.group(2), multipack.group(3));
            if (each.isPresent()) {
                BigDecimal packs = new BigDecimal(multipack.group(1));
                return Optional.of(new PackageSize(each.get().quantity().multiply(packs), each.get().unit()));
            }
        }

        // The first amount we can price wins: "6 ct / 1.5 oz" is six items.
        Matcher amount = AMOUNT.matcher(cleaned);
        while (amount.find()) {
            Optional<PackageSize> size = toSize(amount.group(1), amount.group(2));
            if (size.isPresent()) {
                return size;
            }
        }

        // "each" or "dozen" with no number in front means one of them
        String word = cleaned.replaceAll("[.,]+$", "");
        if (COUNT_WORDS.containsKey(word)) {
            return Optional.of(new PackageSize(COUNT_WORDS.get(word), Unit.COUNT));
        }
        return Optional.empty();
    }

    private static Optional<PackageSize> toSize(String number, String unitWord) {
        BigDecimal quantity = toNumber(number);
        if (quantity.signum() <= 0) {
            return Optional.empty();
        }
        String word = unitWord.replaceAll("[.,]+$", "");
        if (COUNT_WORDS.containsKey(word)) {
            return Optional.of(new PackageSize(quantity.multiply(COUNT_WORDS.get(word)), Unit.COUNT));
        }
        // "fl. oz", "fl oz", and Kroger's "fo" -> "fl oz", the spelling Unit knows
        String normalized = word.replaceAll("^fl\\.?\\s*oz\\.?$", "fl oz").replaceAll("^fo$", "fl oz");
        return Unit.fromWord(normalized)
                .filter(PRICEABLE::contains)
                .map(unit -> new PackageSize(quantity, unit));
    }

    private static BigDecimal toNumber(String number) {
        int slash = number.indexOf('/');
        if (slash < 0) {
            return new BigDecimal(number);
        }
        BigDecimal denominator = new BigDecimal(number.substring(slash + 1));
        if (denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(number.substring(0, slash)).divide(denominator, 4, RoundingMode.HALF_UP);
    }
}