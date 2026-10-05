package com.forkast.backend.recipe.fit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

class RecipeFitEvaluatorTest {

    private final RecipeFitEvaluator evaluator = new RecipeFitEvaluator();

    private static RecipeNutrition nutrition(String calories, String protein, String carbs, String fat) {
        return new RecipeNutrition(dec(calories), dec(protein), dec(carbs), dec(fat), true);
    }

    private static BigDecimal dec(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    @Test
    void overCalorieLimitSaysByHowMuch() {
        FitResult result = evaluator.evaluate(nutrition("720", "40", "50", "20"),
                new FitTargets(600, null, null, null));

        assertThat(result.flags()).extracting(FitFlag::code).containsExactly(FitFlag.Code.OVER_MAX_CALORIES);
        assertThat(result.flags().get(0).message()).isEqualTo("Exceeds your 600 kcal limit by 120");
        assertThat(result.penalty()).isEqualTo(0.2);
    }

    @Test
    void proteinIsFlaggedOnlyWhenUnder80Percent() {
        FitTargets targets = new FitTargets(null, 30, null, null); // 80% of 30 = 24

        FitResult low = evaluator.evaluate(nutrition("500", "23", "50", "20"), targets);
        FitResult close = evaluator.evaluate(nutrition("500", "25", "50", "20"), targets);

        assertThat(low.flags()).extracting(FitFlag::code).containsExactly(FitFlag.Code.UNDER_PROTEIN_TARGET);
        assertThat(low.flags().get(0).message()).isEqualTo("23 g protein, under your 30 g target");
        assertThat(close.fits()).isTrue();
    }

    @Test
    void carbsAndFatAreFlaggedOnlyWhenOver125Percent() {
        FitTargets targets = new FitTargets(null, null, 60, 20); // limits 75 g carbs, 25 g fat

        FitResult over = evaluator.evaluate(nutrition("500", "30", "95", "40"), targets);
        FitResult close = evaluator.evaluate(nutrition("500", "30", "74", "25"), targets);

        assertThat(over.flags()).extracting(FitFlag::code)
                .containsExactly(FitFlag.Code.OVER_CARBS_TARGET, FitFlag.Code.OVER_FAT_TARGET);
        assertThat(over.flags().get(0).message()).isEqualTo("95 g carbs, over your 60 g target");
        assertThat(close.fits()).isTrue();
    }

    @Test
    void unsetPreferencesRaiseNoFlags() {
        FitResult result = evaluator.evaluate(nutrition("2000", "1", "300", "150"), FitTargets.NONE);

        assertThat(result.fits()).isTrue();
        assertThat(result.penalty()).isZero();
    }

    @Test
    void missingNutrientValuesAreSkipped() {
        FitResult result = evaluator.evaluate(nutrition(null, null, null, null),
                new FitTargets(600, 30, 60, 20));

        assertThat(result.fits()).isTrue();
    }

    @Test
    void incompleteNutritionIsFlagged() {
        RecipeNutrition incomplete = new RecipeNutrition(dec("400"), dec("30"), dec("40"), dec("10"), false);

        FitResult result = evaluator.evaluate(incomplete, FitTargets.NONE);

        assertThat(result.flags()).extracting(FitFlag::code).containsExactly(FitFlag.Code.NUTRITION_INCOMPLETE);
        assertThat(result.penalty()).isEqualTo(RecipeFitEvaluator.INCOMPLETE_PENALTY);
    }

    @Test
    void penaltyOrdersPerfectFitFirstAndBigMissLast() {
        FitTargets targets = new FitTargets(600, 30, null, null);
        FitResult perfect = evaluator.evaluate(nutrition("550", "35", "50", "20"), targets);
        FitResult slightlyOver = evaluator.evaluate(nutrition("660", "35", "50", "20"), targets); // 0.1
        FitResult wayOff = evaluator.evaluate(nutrition("900", "10", "50", "20"), targets); // 0.5 + 0.667

        List<FitResult> sorted = List.of(wayOff, perfect, slightlyOver).stream()
                .sorted(Comparator.comparingDouble(FitResult::penalty))
                .toList();

        assertThat(sorted).containsExactly(perfect, slightlyOver, wayOff);
    }
}