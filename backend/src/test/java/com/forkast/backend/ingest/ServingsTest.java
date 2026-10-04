package com.forkast.backend.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ServingsTest {

    @ParameterizedTest
    @CsvSource({
            "6 servings,       6",
            "Makes 12 muffins, 12",
            "4-6,              4",
            "Serves 2,         2",
            "8,                8",
    })
    void readsTheFirstNumber(String yields, int expected) {
        assertThat(Servings.parse(yields)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "a few", "0 servings" })
    void noUsableNumberIsNull(String yields) {
        assertThat(Servings.parse(yields)).isNull();
    }
}