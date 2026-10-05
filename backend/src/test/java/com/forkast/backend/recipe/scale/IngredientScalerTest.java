package com.forkast.backend.recipe.scale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import com.forkast.backend.ingest.Unit;

class IngredientScalerTest {

    @Test
    void scalesByServingsOverBaseServings() {
        BigDecimal third = IngredientScaler.factor(2, 6);

        ScaledAmount can = IngredientScaler.scale(new BigDecimal("1"), "can", third);
        ScaledAmount grams = IngredientScaler.scale(new BigDecimal("450"), "g", third);

        assertThat(can.amount()).isEqualByComparingTo("0.333");
        assertThat(can.displayAmount()).isEqualTo("1/3");
        assertThat(grams.amount()).isEqualByComparingTo("150");
        assertThat(grams.displayAmount()).isEqualTo("150");
    }

    @Test
    void lineWithNoAmountStaysEmpty() {
        ScaledAmount salt = IngredientScaler.scale(null, "to_taste", IngredientScaler.factor(2, 6));

        assertThat(salt.amount()).isNull();
        assertThat(salt.displayAmount()).isNull();
        assertThat(salt.unit()).isEqualTo("to_taste");
    }

    @Test
    void smallCupAmountsStepDownToSpoons() {
        BigDecimal third = IngredientScaler.factor(2, 6);

        ScaledAmount oil = IngredientScaler.scale(new BigDecimal("0.25"), "cup", third); // 1/12 cup
        ScaledAmount cheese = IngredientScaler.scale(new BigDecimal("0.5"), "cup", third); // 1/6 cup
        ScaledAmount vinegar = IngredientScaler.scale(new BigDecimal("1"), "tbsp", third); // 1/3 tbsp
        ScaledAmount stock = IngredientScaler.scale(new BigDecimal("3"), "cup", third); // 1 cup stays

        assertThat(oil.unit()).isEqualTo("tbsp");
        assertThat(oil.displayAmount()).isEqualTo("1 1/3");
        assertThat(cheese.unit()).isEqualTo("tbsp");
        assertThat(cheese.displayAmount()).isEqualTo("2 2/3");
        assertThat(vinegar.unit()).isEqualTo("tsp");
        assertThat(vinegar.displayAmount()).isEqualTo("1");
        assertThat(stock.unit()).isEqualTo("cup");
        assertThat(stock.displayAmount()).isEqualTo("1");
    }

    @Test
    void servingsMustBePositive() {
        assertThatThrownBy(() -> IngredientScaler.factor(0, 4)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
            // amount, unit, display
            "2,      CUP,   2",
            "1.4,    CUP,   1 1/3",
            "0.5,    TSP,   1/2",
            "2.75,   TBSP,  2 3/4",
            "0.95,   CUP,   1",
            "0.667,  CAN,   2/3",
            "0.01,   TSP,   1/8",
            "151.33, G,     151",
            "0.2,    G,     1",
            "0.333,  LB,    0.33",
            "1.5,    OZ,    1.5",
            "2.000,  KG,    2",
    })
    void displaysTheWayACookReadsIt(String amount, Unit unit, String expected) {
        assertThat(IngredientScaler.display(new BigDecimal(amount), unit)).isEqualTo(expected);
    }
}