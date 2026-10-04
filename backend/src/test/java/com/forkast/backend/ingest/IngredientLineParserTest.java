package com.forkast.backend.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class IngredientLineParserTest {

    private final IngredientLineParser parser = new IngredientLineParser();

    // ---------- the cases from the plan doc's parser table ----------

    @Test
    void wholeNumberAndUnit() {
        assertLine(parser.parse("2 cups flour"), "2", Unit.CUP, "flour", null);
    }

    @Test
    void mixedFraction() {
        assertLine(parser.parse("1 1/2 tsp salt"), "1.5", Unit.TSP, "salt", null);
    }

    @Test
    void unicodeFraction() {
        assertLine(parser.parse("½ cup sugar"), "0.5", Unit.CUP, "sugar", null);
    }

    @Test
    void rangeUsesUpperNumber() {
        assertLine(parser.parse("2-3 cloves garlic, minced"), "3", Unit.CLOVE, "garlic", "minced");
    }

    @Test
    void containerSizeTimesCount() {
        assertLine(parser.parse("1 (15 oz) can black beans, drained"), "15", Unit.OZ, "black beans", "drained");
    }

    @Test
    void sizeWordBecomesPrepNote() {
        assertLine(parser.parse("3 large eggs"), "3", Unit.COUNT, "eggs", "large");
    }

    @Test
    void trailingDotOnUnitIgnored() {
        assertLine(parser.parse("8 oz. cream cheese, softened"), "8", Unit.OZ, "cream cheese", "softened");
    }

    @Test
    void optionalLine() {
        ParsedLine line = parser.parse("1/4 cup parsley, chopped (optional)");

        assertLine(line, "0.25", Unit.CUP, "parsley", "chopped");
        assertThat(line.optional()).isTrue();
    }

    @Test
    void toTasteHasNoAmount() {
        assertLine(parser.parse("Salt and pepper to taste"), null, Unit.TO_TASTE, "salt and pepper", null);
    }

    @Test
    void sectionHeaderIsSkipped() {
        assertThat(parser.parse("For the sauce:").skip()).isTrue();
    }

    // ---------- extra cases real recipe sites produce ----------

    @Test
    void unicodeFractionAttachedToWholeNumber() {
        assertLine(parser.parse("1½ cups milk"), "1.5", Unit.CUP, "milk", null);
    }

    @Test
    void hyphenatedMixedNumber() {
        assertLine(parser.parse("1-1/2 cups water"), "1.5", Unit.CUP, "water", null);
    }

    @Test
    void multipleContainersWithHyphenatedSize() {
        assertLine(parser.parse("2 (14.5-ounce) cans diced tomatoes"), "29", Unit.OZ, "diced tomatoes", null);
    }

    @Test
    void articleMeansOne() {
        assertLine(parser.parse("a pinch of salt"), "1", Unit.PINCH, "salt", null);
    }

    @Test
    void capitalTIsTablespoonLowercaseTIsTeaspoon() {
        assertThat(parser.parse("1 T butter").unit()).isEqualTo(Unit.TBSP);
        assertThat(parser.parse("1 t vanilla").unit()).isEqualTo(Unit.TSP);
    }

    @Test
    void sizeWordBeforeUnit() {
        assertLine(parser.parse("2 large cloves garlic"), "2", Unit.CLOVE, "garlic", "large");
    }

    @Test
    void descriptorWordsStayInNameForTheMatcher() {
        // "crushed tomatoes" is its own catalog ingredient, so the parser can't strip
        // "chopped"/"crushed" safely. IngredientMatcher tries the name with and without
        // them.
        assertLine(parser.parse("2 1/2 cups chopped onions, divided"), "2.5", Unit.CUP, "chopped onions", "divided");
    }

    @Test
    void noAmountLeavesAmountAndUnitNull() {
        assertLine(parser.parse("Cooking spray"), null, null, "cooking spray", null);
    }

    @Test
    void amountWithToTasteKeepsTheAmount() {
        assertLine(parser.parse("1 tsp salt, or to taste"), "1", Unit.TSP, "salt", null);
    }

    // ---------- patterns found in the first real scrape ----------

    @Test
    void commaBetweenDescriptorsStaysInName() {
        assertLine(parser.parse("1 lb. boneless, skinless chicken breast ($4.99)"),
                "1", Unit.LB, "boneless skinless chicken breast", "$4.99");
    }

    @Test
    void commaAfterRealNameStillSplits() {
        assertLine(parser.parse("1 medium onion, diced"), "1", Unit.COUNT, "onion", "medium, diced");
    }

    @Test
    void containerSizeWithoutParentheses() {
        assertLine(parser.parse("1 10oz. can diced tomatoes with green chiles ($0.96**)"),
                "10", Unit.OZ, "diced tomatoes with green chiles", "$0.96");
        assertLine(parser.parse("1 28-ounce can crushed plum tomatoes"),
                "28", Unit.OZ, "crushed plum tomatoes", null);
    }

    @Test
    void containerWordAfterUnitIsDropped() {
        assertLine(parser.parse("4 oz. can fire roasted green chiles (with juices, $0.88)"),
                "4", Unit.OZ, "fire roasted green chiles", "with juices, $0.88");
    }

    @Test
    void largeVolumeUnits() {
        assertLine(parser.parse("1 gallon water"), "1", Unit.GALLON, "water", null);
        assertLine(parser.parse("1 pint grape tomatoes ($1.69)"), "1", Unit.PINT, "grape tomatoes", "$1.69");
    }

    @Test
    void pinchWithoutAmountMeansOne() {
        assertLine(parser.parse("pinch salt ($0.02)"), "1", Unit.PINCH, "salt", "$0.02");
    }

    @Test
    void descriptorBeforeUnitGoesToNote() {
        assertLine(parser.parse("1/4 packed cup basil leaves (hand-torn)"),
                "0.25", Unit.CUP, "basil leaves", "packed, hand-torn");
    }

    @ParameterizedTest
    @CsvSource({
            "1 qt oil,           QUART",
            "1 tablespoon oil,   TBSP",
            "1 Tbsp oil,         TBSP",
            "1 teaspoons oil,    TSP",
            "1 lbs oil,          LB",
            "1 pound oil,        LB",
            "1 fl oz oil,        FL_OZ",
            "1 fluid ounces oil, FL_OZ",
            "1 grams oil,        G",
            "1 ml oil,           ML",
            "1 c. oil,           CUP",
    })
    void unitSpellingsNormalize(String line, Unit expected) {
        ParsedLine parsed = parser.parse(line);

        assertThat(parsed.unit()).isEqualTo(expected);
        assertThat(parsed.name()).isEqualTo("oil");
    }

    // ---------- helper ----------

    private static void assertLine(ParsedLine line, String amount, Unit unit, String name, String prepNote) {
        if (amount == null) {
            assertThat(line.amount()).isNull();
        } else {
            // BigDecimal.equals compares scale too (1.5 != 1.50), so compare by value
            assertThat(line.amount()).isEqualByComparingTo(amount);
        }
        assertThat(line.unit()).isEqualTo(unit);
        assertThat(line.name()).isEqualTo(name);
        assertThat(line.prepNote()).isEqualTo(prepNote);
        assertThat(line.skip()).isFalse();
    }
}