package com.forkast.backend.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.forkast.backend.ingest.Unit;

class PackageSizeParserTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // text | quantity | unit
            "1 lb              | 1        | LB", // Kroger by-weight items
            "3 lb              | 3        | LB", // the onion bag from the test run
            "16 oz             | 16       | OZ",
            "15.5 oz           | 15.5     | OZ",
            "32 fl oz          | 32       | FL_OZ",
            "16.9 fl. oz       | 16.9     | FL_OZ",
            "30 fo             | 30       | FL_OZ", // Kroger's mayonnaise label
            "1 gal             | 1        | GALLON",
            "1/2 gal           | 0.5      | GALLON",
            "1.5 L             | 1.5      | L",
            "500 g             | 500      | G",
            "2 lbs             | 2        | LB",
            "12 ct             | 12       | COUNT",
            "18 count          | 18       | COUNT",
            "1 dozen           | 12       | COUNT",
            "each              | 1        | COUNT",
            "about 2.5 lb      | 2.5      | LB",
            "12 x 12 fl oz     | 144      | FL_OZ", // a multipack is its total
            "6 ct / 1.5 oz     | 6        | COUNT", // first amount wins
            "1 LB              | 1        | LB",
    })
    void readsSizes(String text, String quantity, Unit unit) {
        assertThat(PackageSizeParser.parse(text)).hasValueSatisfying(size -> {
            assertThat(size.quantity()).isEqualByComparingTo(quantity);
            assertThat(size.unit()).isEqualTo(unit);
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "family size", "1 can", "0 oz", "1/0 lb", "large" })
    void unreadableSizesAreEmpty(String text) {
        assertThat(PackageSizeParser.parse(text)).isEmpty();
    }
}