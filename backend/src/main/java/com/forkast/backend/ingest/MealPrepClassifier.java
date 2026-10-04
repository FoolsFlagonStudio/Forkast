package com.forkast.backend.ingest;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

/**
 * Scores how well a recipe holds up when cooked ahead, from 0 to 100. Starts at
 * 40, adds or
 * subtracts points for each signal (each applies at most once), then clamps.
 */
@Component
public class MealPrepClassifier {

    static final int BASE_SCORE = 40;

    private static final Pattern MEAL_PREP_WORDS = Pattern.compile(
            "meal[- ]?prep|make[- ]ahead|freezer|batch", Pattern.CASE_INSENSITIVE);
    private static final Pattern STORAGE_WORDS = Pattern.compile(
            "refrigerat|airtight|freez|keeps? (?:well )?for|up to \\d+ days", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISH_TYPES = Pattern.compile(
            "\\b(?:soups?|stews?|chil(?:i|e)s?|curr(?:y|ies)|casseroles?|bowls?|brais(?:e|ed)|bakes?|lasagnas?)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SERVE_IMMEDIATELY = Pattern.compile("serve immediately", Pattern.CASE_INSENSITIVE);
    private static final Pattern FRIED = Pattern.compile(
            "\\b(?:fried|deep[- ]fr(?:y|ied)|crispy)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SALAD = Pattern.compile("\\bsalads?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEAFY_GREENS = Pattern.compile(
            "\\b(?:lettuce|romaine|arugula|greens|spring mix|mesclun)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FRAGILE_INGREDIENTS = Pattern.compile(
            "\\b(?:avocados?|sashimi|sushi[- ]grade|raw (?:tuna|salmon|fish))\\b", Pattern.CASE_INSENSITIVE);

    public int score(MealPrepInput recipe) {
        String nameAndCategory = join(recipe.name(), recipe.category());
        String keywordsAndCategory = join(recipe.keywords(), recipe.category());
        String instructions = join(recipe.instructions());
        String ingredients = join(recipe.ingredientNames());

        int score = BASE_SCORE;

        if (MEAL_PREP_WORDS.matcher(keywordsAndCategory).find()) {
            score += 30;
        }
        if (STORAGE_WORDS.matcher(instructions).find()) {
            score += 15;
        }
        if (DISH_TYPES.matcher(nameAndCategory).find()) {
            score += 15;
        }
        if (recipe.baseServings() >= 4) {
            score += 5;
        }
        if (SERVE_IMMEDIATELY.matcher(instructions).find()) {
            score -= 25;
        }
        if (FRIED.matcher(join(recipe.name(), instructions)).find()) {
            score -= 15;
        }
        boolean leafySalad = SALAD.matcher(nameAndCategory).find() && LEAFY_GREENS.matcher(ingredients).find();
        if (leafySalad || FRAGILE_INGREDIENTS.matcher(ingredients).find()) {
            score -= 15;
        }

        return Math.max(0, Math.min(100, score));
    }

    // ---------- helpers ----------

    private static String join(String... parts) {
        return Stream.of(parts).filter(Objects::nonNull).collect(Collectors.joining(" "));
    }

    private static String join(List<String> parts) {
        return parts == null ? "" : parts.stream().filter(Objects::nonNull).collect(Collectors.joining(" "));
    }
}