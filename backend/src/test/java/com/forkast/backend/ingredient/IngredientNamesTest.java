package com.forkast.backend.ingredient;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class IngredientNamesTest {

    @Test
    void candidatesGoFromMostToLeastSpecific() {
        assertThat(IngredientNames.candidates("Boneless Skinless Chicken Breasts")).containsExactly(
                "boneless skinless chicken breasts",
                "boneless skinless chicken breast",
                "chicken breasts",
                "chicken breast");
    }

    @Test
    void duplicatesAreDropped() {
        // already singular and nothing to strip, so there's only one candidate
        assertThat(IngredientNames.candidates("garlic")).containsExactly("garlic");
    }

    @Test
    void percentagesAndDescriptorsAreStripped() {
        assertThat(IngredientNames.candidates("93% lean ground turkey")).contains("ground turkey");
    }

    @Test
    void groundIsKeptBecauseItChangesTheIngredient() {
        assertThat(IngredientNames.candidates("freshly ground black pepper")).contains("ground black pepper");
    }

    @Test
    void crackedAndReservedAreDescriptors() {
        assertThat(IngredientNames.candidates("freshly cracked pepper")).contains("pepper");
        assertThat(IngredientNames.candidates("reserved pasta water")).contains("pasta water");
    }

    @Test
    void blankNameHasNoCandidates() {
        assertThat(IngredientNames.candidates("  ")).isEmpty();
        assertThat(IngredientNames.candidates(null)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "carrots,          carrot",
            "tomatoes,         tomato",
            "blueberries,      blueberry",
            "peaches,          peach",
            "cherry tomatoes,  cherry tomato",
            "glass,            glass",
            "peas,             pea",
            "gas,              gas",
    })
    void singularOfLastWord(String plural, String expected) {
        assertThat(IngredientNames.singular(plural)).isEqualTo(expected);
    }
}