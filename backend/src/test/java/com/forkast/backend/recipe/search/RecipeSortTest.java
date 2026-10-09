package com.forkast.backend.recipe.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.forkast.backend.common.exception.ApiException;

class RecipeSortTest {

    @ParameterizedTest
    @CsvSource({
            "fit,       FIT",
            "Protein,   PROTEIN",
            "mealPrep,  MEAL_PREP",
            "meal_prep, MEAL_PREP",
            "NEWEST,    NEWEST",
            "cost,      COST",
    })
    void acceptsTheUsualSpellings(String value, RecipeSort expected) {
        assertThat(RecipeSort.from(value)).isEqualTo(expected);
    }

    @Test
    void missingMeansFit() {
        assertThat(RecipeSort.from(null)).isEqualTo(RecipeSort.FIT);
        assertThat(RecipeSort.from(" ")).isEqualTo(RecipeSort.FIT);
    }

    @Test
    void unknownSortIsABadRequest() {
        assertThatThrownBy(() -> RecipeSort.from("price")).isInstanceOf(ApiException.class);
    }
}