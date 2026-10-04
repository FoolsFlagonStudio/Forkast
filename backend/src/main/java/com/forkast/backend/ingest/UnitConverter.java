package com.forkast.backend.ingest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientPortion;

/**
 * Converts a parsed amount and unit into grams for one ingredient.
 *
 * Mass units use fixed factors. Volume and item units ("cup", "clove", "2 large
 * eggs") need
 * the ingredient's FDC portions, because a cup of flour and a cup of milk weigh
 * different
 * amounts. Returns empty when there's no way to know the weight.
 */
@Component
public class UnitConverter {

    private static final Map<Unit, BigDecimal> GRAMS_PER_UNIT = Map.of(
            Unit.G, new BigDecimal("1"),
            Unit.KG, new BigDecimal("1000"),
            Unit.OZ, new BigDecimal("28.35"),
            Unit.LB, new BigDecimal("453.59"));

    private static final Map<Unit, BigDecimal> ML_PER_UNIT = Map.of(
            Unit.TSP, new BigDecimal("4.929"),
            Unit.TBSP, new BigDecimal("14.787"),
            Unit.CUP, new BigDecimal("236.588"),
            Unit.FL_OZ, new BigDecimal("29.574"),
            Unit.PINT, new BigDecimal("473.176"),
            Unit.QUART, new BigDecimal("946.353"),
            Unit.GALLON, new BigDecimal("3785.41"),
            Unit.ML, new BigDecimal("1"),
            Unit.L, new BigDecimal("1000"));

    private static final List<String> SIZE_WORDS = List.of("extra large", "extra-large", "jumbo", "large", "medium",
            "small");

    /** Used for "2 onions" when the recipe gives no size. */
    private static final List<String> DEFAULT_SIZES = List.of("medium", "large", "small");

    /** Portions that hold many items, so they are never "one" of something. */
    private static final Set<String> BULK_WORDS = Set.of("package", "pkg", "bunch", "container", "bag", "box",
            "serving", "nlea");

    public Optional<BigDecimal> toGrams(BigDecimal amount, Unit unit, String prepNote, Ingredient ingredient) {
        if (amount == null || unit == null || ingredient == null) {
            return Optional.empty();
        }
        if (GRAMS_PER_UNIT.containsKey(unit)) {
            return Optional.of(round(amount.multiply(GRAMS_PER_UNIT.get(unit))));
        }
        if (ML_PER_UNIT.containsKey(unit)) {
            return volumeToGrams(amount, unit, prepNote, ingredient.getPortions());
        }
        return switch (unit) {
            case COUNT, CLOVE, SLICE, CAN -> itemToGrams(amount, unit, prepNote, ingredient.getPortions());
            default -> Optional.empty(); // PINCH and TO_TASTE have no meaningful weight
        };
    }

    // ---------- volume ----------

    private Optional<BigDecimal> volumeToGrams(BigDecimal amount, Unit unit, String prepNote,
            List<IngredientPortion> portions) {
        // 1. A portion in the same unit: "1 cup" -> the ingredient's "cup, chopped"
        // portion.
        List<IngredientPortion> sameUnit = portions.stream()
                .filter(p -> portionUnit(p) == unit)
                .toList();
        if (!sameUnit.isEmpty()) {
            IngredientPortion portion = preferMatchingNote(sameUnit, prepNote);
            return Optional.of(round(amount.multiply(portion.getGramWeight())));
        }

        // 2. Any volume portion, scaled by millilitres: 1 tbsp of something with only a
        // "cup" portion.
        for (IngredientPortion portion : portions) {
            Unit portionUnit = portionUnit(portion);
            if (portionUnit != null && ML_PER_UNIT.containsKey(portionUnit)) {
                BigDecimal gramsPerMl = portion.getGramWeight()
                        .divide(ML_PER_UNIT.get(portionUnit), 6, RoundingMode.HALF_UP);
                return Optional.of(round(amount.multiply(ML_PER_UNIT.get(unit)).multiply(gramsPerMl)));
            }
        }
        return Optional.empty();
    }

    // ---------- items ----------

    private Optional<BigDecimal> itemToGrams(BigDecimal amount, Unit unit, String prepNote,
            List<IngredientPortion> portions) {
        // 1. The size the recipe asked for: "3 large eggs" -> the "large" portion.
        String size = sizeIn(prepNote);
        if (size != null) {
            Optional<IngredientPortion> sized = firstStartingWith(portions, size);
            if (sized.isPresent()) {
                return Optional.of(round(amount.multiply(sized.get().getGramWeight())));
            }
        }

        // 2. A portion named for the unit: "2 cloves garlic" -> the "clove" portion.
        if (unit != Unit.COUNT) {
            for (IngredientPortion portion : portions) {
                if (portionUnit(portion) == unit) {
                    return Optional.of(round(amount.multiply(portion.getGramWeight())));
                }
            }
        }

        // 3. A typical size: medium, then large, then small.
        for (String defaultSize : DEFAULT_SIZES) {
            Optional<IngredientPortion> sized = firstStartingWith(portions, defaultSize);
            if (sized.isPresent()) {
                return Optional.of(round(amount.multiply(sized.get().getGramWeight())));
            }
        }

        // 4. The first portion that is one item ("piece", "stalk"), not a measure or a
        // package.
        for (IngredientPortion portion : portions) {
            Unit portionUnit = portionUnit(portion);
            boolean isMeasure = portionUnit != null
                    && (GRAMS_PER_UNIT.containsKey(portionUnit) || ML_PER_UNIT.containsKey(portionUnit));
            if (!isMeasure && !BULK_WORDS.contains(firstWord(portion))) {
                return Optional.of(round(amount.multiply(portion.getGramWeight())));
            }
        }
        return Optional.empty();
    }

    // ---------- helpers ----------

    /**
     * The unit a portion description starts with: "cup, chopped" -> CUP, "fl oz" ->
     * FL_OZ.
     */
    static Unit portionUnit(IngredientPortion portion) {
        String[] words = portion.getDescription().toLowerCase(Locale.ROOT).split("[\\s,(]+");
        if (words.length >= 2) {
            Optional<Unit> twoWord = Unit.fromWord(words[0] + " " + words[1]);
            if (twoWord.isPresent()) {
                return twoWord.get();
            }
        }
        return words.length == 0 ? null : Unit.fromWord(words[0]).orElse(null);
    }

    private static String firstWord(IngredientPortion portion) {
        String[] words = portion.getDescription().toLowerCase(Locale.ROOT).split("[\\s,(]+");
        return words.length == 0 ? "" : words[0];
    }

    private static String sizeIn(String prepNote) {
        if (prepNote == null) {
            return null;
        }
        String note = prepNote.toLowerCase(Locale.ROOT);
        return SIZE_WORDS.stream().filter(note::contains).findFirst().orElse(null);
    }

    private static Optional<IngredientPortion> firstStartingWith(List<IngredientPortion> portions, String prefix) {
        return portions.stream()
                .filter(p -> p.getDescription().toLowerCase(Locale.ROOT).startsWith(prefix))
                .findFirst();
    }

    /**
     * Among same-unit portions, prefer one that mentions a word from the prep note
     * ("chopped").
     */
    private static IngredientPortion preferMatchingNote(List<IngredientPortion> portions, String prepNote) {
        if (prepNote != null) {
            for (String word : prepNote.toLowerCase(Locale.ROOT).split("[\\s,]+")) {
                if (word.length() < 4) {
                    continue; // skip "and", "or", "to"
                }
                for (IngredientPortion portion : portions) {
                    if (portion.getDescription().toLowerCase(Locale.ROOT).contains(word)) {
                        return portion;
                    }
                }
            }
        }
        return portions.get(0);
    }

    private static BigDecimal round(BigDecimal grams) {
        return grams.setScale(2, RoundingMode.HALF_UP);
    }
}