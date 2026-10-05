package com.forkast.backend.recipe.fit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Compares one recipe's per-serving nutrition with the user's per-meal targets.
 *
 * Plain Java, no database. Dietary restrictions are not checked here:
 * restricted recipes are
 * filtered out by the query before this runs. The meal plan phase reuses this
 * with the
 * week's remaining budget and calories added.
 */
@Component
public class RecipeFitEvaluator {

    /**
     * Protein below 80% of the target is flagged; 24 g against 30 g is close
     * enough.
     */
    static final BigDecimal PROTEIN_TOLERANCE = new BigDecimal("0.80");

    /** Carbs or fat above 125% of the target is flagged. */
    static final BigDecimal LIMIT_TOLERANCE = new BigDecimal("1.25");

    static final double INCOMPLETE_PENALTY = 0.1;

    public FitResult evaluate(RecipeNutrition nutrition, FitTargets targets) {
        List<FitFlag> flags = new ArrayList<>();
        double penalty = 0;

        // Calories: any amount over the limit counts, since it's a limit the user
        // chose.
        if (isSet(targets.maxCaloriesPerMeal()) && nutrition.calories() != null) {
            BigDecimal limit = BigDecimal.valueOf(targets.maxCaloriesPerMeal());
            if (nutrition.calories().compareTo(limit) > 0) {
                BigDecimal over = nutrition.calories().subtract(limit);
                flags.add(new FitFlag(FitFlag.Code.OVER_MAX_CALORIES,
                        "Exceeds your " + whole(limit) + " kcal limit by " + whole(over)));
                penalty += fraction(over, limit);
            }
        }

        // Protein: a minimum, flagged when well under.
        if (isSet(targets.proteinGrams()) && nutrition.protein() != null) {
            BigDecimal target = BigDecimal.valueOf(targets.proteinGrams());
            if (nutrition.protein().compareTo(target.multiply(PROTEIN_TOLERANCE)) < 0) {
                flags.add(new FitFlag(FitFlag.Code.UNDER_PROTEIN_TARGET,
                        whole(nutrition.protein()) + " g protein, under your " + whole(target) + " g target"));
                penalty += fraction(target.subtract(nutrition.protein()), target);
            }
        }

        // Carbs and fat: soft maximums, flagged when well over.
        penalty += checkLimit(nutrition.carbs(), targets.carbsGrams(), "carbs", FitFlag.Code.OVER_CARBS_TARGET, flags);
        penalty += checkLimit(nutrition.fat(), targets.fatGrams(), "fat", FitFlag.Code.OVER_FAT_TARGET, flags);

        if (!nutrition.complete()) {
            flags.add(new FitFlag(FitFlag.Code.NUTRITION_INCOMPLETE,
                    "Nutrition is an estimate: some ingredients aren't matched yet"));
            penalty += INCOMPLETE_PENALTY;
        }

        return new FitResult(List.copyOf(flags), penalty);
    }

    // ---------- helpers ----------

    private static double checkLimit(BigDecimal value, Integer targetGrams, String label,
            FitFlag.Code code, List<FitFlag> flags) {
        if (!isSet(targetGrams) || value == null) {
            return 0;
        }
        BigDecimal target = BigDecimal.valueOf(targetGrams);
        if (value.compareTo(target.multiply(LIMIT_TOLERANCE)) <= 0) {
            return 0;
        }
        flags.add(new FitFlag(code, whole(value) + " g " + label + ", over your " + whole(target) + " g target"));
        return fraction(value.subtract(target), target);
    }

    private static boolean isSet(Integer target) {
        return target != null && target > 0;
    }

    /** How far off, as a fraction of the target: 120 over a 600 limit is 0.2. */
    private static double fraction(BigDecimal difference, BigDecimal target) {
        return difference.divide(target, 4, RoundingMode.HALF_UP).doubleValue();
    }

    private static String whole(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}