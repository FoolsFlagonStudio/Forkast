package com.forkast.backend.ingest;

import static com.forkast.backend.ingest.TestIngredients.withPortions;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.forkast.backend.ingredient.Ingredient;

class UnitConverterTest {

    private final UnitConverter converter = new UnitConverter();

    private Optional<BigDecimal> grams(String amount, Unit unit, String prepNote, Ingredient ingredient) {
        return converter.toGrams(new BigDecimal(amount), unit, prepNote, ingredient);
    }

    // ---------- mass ----------

    @Test
    void massUsesFixedFactors() {
        Ingredient beef = withPortions("ground beef");

        assertThat(grams("1", Unit.LB, null, beef))
                .hasValueSatisfying(g -> assertThat(g).isEqualByComparingTo("453.59"));
        assertThat(grams("8", Unit.OZ, null, beef))
                .hasValueSatisfying(g -> assertThat(g).isEqualByComparingTo("226.80"));
    }

    // ---------- volume ----------

    @Test
    void volumeUsesPortionInSameUnit() {
        Ingredient flour = withPortions("flour", "cup", "125");

        assertThat(grams("2", Unit.CUP, null, flour))
                .hasValueSatisfying(g -> assertThat(g).isEqualByComparingTo("250"));
    }

    @Test
    void volumePrefersPortionMatchingPrepNote() {
        Ingredient onion = withPortions("onion", "cup, chopped", "160", "cup, sliced", "115");

        assertThat(grams("1", Unit.CUP, "sliced", onion))
                .hasValueSatisfying(g -> assertThat(g).isEqualByComparingTo("115"));
    }

    @Test
    void volumeScalesFromAnotherVolumePortion() {
        // 125 g per cup / 236.588 ml per cup * 14.787 ml per tbsp = 7.81 g
        Ingredient flour = withPortions("flour", "cup", "125");

        assertThat(grams("1", Unit.TBSP, null, flour))
                .hasValueSatisfying(g -> assertThat(g).isEqualByComparingTo("7.81"));
    }

    @Test
    void pintScalesFromCupPortion() {
        // 473.176 ml per pint / 236.588 ml per cup = 2 cups
        Ingredient water = withPortions("water", "cup", "237");

        assertThat(grams("1", Unit.PINT, null, water))
                .hasValueSatisfying(g -> assertThat(g).isEqualByComparingTo("474"));
    }

    @Test
    void volumeWithNoVolumePortionIsUnknown() {
        Ingredient chicken = withPortions("chicken breast", "oz", "28.25", "piece", "272");

        assertThat(grams("1", Unit.CUP, null, chicken)).isEmpty();
    }

    // ---------- items ----------

    @Test
    void itemUsesRequestedSize() {
        Ingredient egg = withPortions("egg", "medium", "44", "large", "50");

        assertThat(grams("3", Unit.COUNT, "large", egg))
                .hasValueSatisfying(g -> assertThat(g).isEqualByComparingTo("150"));
    }

    @Test
    void itemWithoutSizeDefaultsToMedium() {
        Ingredient egg = withPortions("egg", "large", "50", "medium", "44");

        assertThat(grams("2", Unit.COUNT, null, egg)).hasValueSatisfying(g -> assertThat(g).isEqualByComparingTo("88"));
    }

    @Test
    void cloveUsesClovePortion() {
        Ingredient garlic = withPortions("garlic", "cup", "136", "clove", "3");

        assertThat(grams("2", Unit.CLOVE, "minced", garlic))
                .hasValueSatisfying(g -> assertThat(g).isEqualByComparingTo("6"));
    }

    @Test
    void itemFallsBackToFirstSingleItemPortion() {
        // "oz" is a measure and "package" holds many, so "piece" is the one item
        Ingredient chicken = withPortions("chicken breast", "oz", "28.25", "package", "926", "piece", "272");

        assertThat(grams("1", Unit.COUNT, null, chicken))
                .hasValueSatisfying(g -> assertThat(g).isEqualByComparingTo("272"));
    }

    // ---------- no weight ----------

    @Test
    void pinchAndToTasteHaveNoWeight() {
        Ingredient salt = withPortions("salt", "tsp", "6");

        assertThat(grams("1", Unit.PINCH, null, salt)).isEmpty();
        assertThat(converter.toGrams(null, Unit.TO_TASTE, null, salt)).isEmpty();
    }

    @Test
    void unmatchedIngredientHasNoWeight() {
        assertThat(grams("1", Unit.CUP, null, null)).isEmpty();
    }
}