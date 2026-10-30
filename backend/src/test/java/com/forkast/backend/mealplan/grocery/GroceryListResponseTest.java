package com.forkast.backend.mealplan.grocery;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GroceryListResponseTest {

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource({
            "113.4,  g,     4 oz",
            "0.58,   g,     0.6 g",
            "10.42,  g,     10.4 g",
            "453.59, g,     1 lb",
            "680.385,g,     1.5 lb",
            "2.5,    count, 2.5",
            "3,      can,   3 can",
            "1.333,  fl_oz, 1.33 fl oz",
    })
    void roundsOnlyForDisplay(String amount, String unit, String expected) {
        assertThat(GroceryListResponse.display(new BigDecimal(amount), unit)).isEqualTo(expected);
    }
}