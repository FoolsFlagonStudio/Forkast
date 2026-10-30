package com.forkast.backend.ingest;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

/**
 * Scores how well a recipe fits breakfast, lunch and dinner, each 0 to 100,
 * from its scraped
 * category, name and keywords. The category is a comma-separated list of tags
 * ("Breakfast,Brunch,Dinner,Lunch"), read as a whole:
 *
 * 1. Not a meal: a side dish, sauce, condiment, dressing or dip scores 0
 * everywhere, even when
 * it's also tagged Dinner (sites tag sides with the meal they go with).
 * Desserts, snacks,
 * appetizers, drinks and baking score 0 only when no meal tag comes with them,
 * so a
 * "Breakfast,Snack" granola is still a breakfast.
 * 2. A starting point: a Breakfast tag leans breakfast, then salads lunch,
 * soups both, and mains
 * (or no category) lunch and dinner. Brunch alone counts as breakfast; next to
 * other tags
 * it adds nothing ("Brunch,Dinner,Lunch" is a lunch or dinner dish).
 * 3. Keyword adjustments from the name and keywords: oats, pancakes, eggs push
 * toward
 * breakfast and away from lunch and dinner; salads, sandwiches and wraps toward
 * lunch;
 * roasts and casseroles toward dinner.
 * 4. A slot the category names outright (Breakfast, Lunch, Dinner) is at least
 * EXPLICIT_FLOOR, so a frittata the site files under Dinner can be a dinner,
 * just a less
 * likely one than a dish that scores 85.
 *
 * Scores are clamped to 0 to 100. A recipe can fill a slot at 50 or more
 * (SlotScores.FITS).
 */
@Component
public class SlotClassifier {

    record Start(int breakfast, int lunch, int dinner) {
    }

    static final Start BREAKFAST_START = new Start(90, 25, 10);
    static final Start SALAD_START = new Start(5, 85, 45);
    static final Start SOUP_START = new Start(5, 70, 70);
    static final Start MAIN_START = new Start(5, 60, 85);

    /** A slot the category names outright is at least this. */
    static final int EXPLICIT_FLOOR = 60;

    private static final Pattern BREAKFAST_CATEGORY = Pattern.compile("\\bbreakfast\\b");
    private static final Pattern BRUNCH_CATEGORY = Pattern.compile("\\bbrunch\\b");
    private static final Pattern LUNCH_CATEGORY = Pattern.compile("\\blunch\\b");
    private static final Pattern DINNER_CATEGORY = Pattern.compile("\\bdinner\\b");
    private static final Pattern SALAD_CATEGORY = Pattern.compile("\\bsalads?\\b");
    private static final Pattern SOUP_CATEGORY = Pattern.compile("\\b(?:soups?|stews?)\\b");
    private static final Pattern MAIN_CATEGORY = Pattern
            .compile("\\b(?:main|mains|entree|entrees|entrée|dinner|lunch)\\b");
    /** Never a meal on its own, whatever else it's tagged */
    private static final Pattern SIDE_CATEGORY = Pattern.compile(
            "\\b(?:sides?|side dish(?:es)?|sauces?|condiments?|dips?|dressings?)\\b");
    /** Not a meal unless a meal tag comes with it */
    private static final Pattern NOT_A_MEAL_CATEGORY = Pattern.compile(
            "\\b(?:desserts?|appetizers?|snacks?|drinks?|beverages?|cocktails?|baking)\\b");

    private static final Pattern BREAKFAST_WORDS = Pattern.compile(
            "\\b(?:breakfast|brunch|oats|oatmeal|pancakes?|waffles?|french toast|granola|muffins?|smoothies?"
                    + "|parfaits?|chia|omelets?|omelettes?|frittatas?|quiches?|shakshuka|scrambled|hash ?browns?|bagels?)\\b");
    /** "eggs" as the dish, not egg noodles or egg rolls */
    private static final Pattern EGG_DISH = Pattern.compile("\\beggs?\\b(?! ?(?:noodles?|rolls?|fried rice|drop))");
    private static final Pattern SALAD_WORDS = Pattern.compile("\\bsalads?\\b");
    private static final Pattern LUNCH_WORDS = Pattern.compile(
            "\\b(?:sandwich(?:es)?|wraps?|paninis?|quesadillas?|pitas?|grain bowls?|meal prep bowls?|lettuce wraps?)\\b");
    private static final Pattern DINNER_WORDS = Pattern.compile(
            "\\b(?:roast(?:ed)?|braised?|casseroles?|lasagnas?|pot roast|sheet pan|slow cooker|steaks?|stir[- ]?fry|curry|curries)\\b");

    public SlotScores score(String name, String category, String keywords) {
        String cat = lower(category);
        String words = lower(join(name, keywords));

        Start start = start(cat);
        if (start == null) {
            return new SlotScores(0, 0, 0);
        }
        int breakfast = start.breakfast();
        int lunch = start.lunch();
        int dinner = start.dinner();

        if (BREAKFAST_WORDS.matcher(words).find()) {
            breakfast += 60; // enough to fit breakfast even with no category
            lunch -= 35; // oatmeal and parfaits aren't lunch unless the site says so
            dinner -= 40;
        } else if (EGG_DISH.matcher(lower(name)).find() && !SALAD_WORDS.matcher(words).find()) {
            breakfast += 20;
        }
        if (SALAD_WORDS.matcher(lower(name)).find()) {
            lunch += 25;
            dinner -= 40; // a salad filed as a main still lands under the dinner cutoff
        } else if (LUNCH_WORDS.matcher(words).find()) {
            lunch += 25;
            dinner -= 30;
        }
        if (DINNER_WORDS.matcher(words).find()) {
            dinner += 15;
        }

        if (BREAKFAST_CATEGORY.matcher(cat).find()) {
            breakfast = Math.max(breakfast, EXPLICIT_FLOOR);
        }
        if (LUNCH_CATEGORY.matcher(cat).find()) {
            lunch = Math.max(lunch, EXPLICIT_FLOOR);
        }
        if (DINNER_CATEGORY.matcher(cat).find()) {
            dinner = Math.max(dinner, EXPLICIT_FLOOR);
        }

        return new SlotScores(clamp(breakfast), clamp(lunch), clamp(dinner));
    }

    /**
     * The category's starting point, or null for things that aren't a meal (sides,
     * desserts).
     */
    private static Start start(String category) {
        if (SIDE_CATEGORY.matcher(category).find()) {
            return null;
        }
        if (BREAKFAST_CATEGORY.matcher(category).find()) {
            return BREAKFAST_START;
        }
        if (SALAD_CATEGORY.matcher(category).find()) {
            return SALAD_START;
        }
        if (SOUP_CATEGORY.matcher(category).find()) {
            return SOUP_START;
        }
        if (MAIN_CATEGORY.matcher(category).find()) {
            return MAIN_START;
        }
        if (BRUNCH_CATEGORY.matcher(category).find()) {
            return BREAKFAST_START; // brunch with no lunch, dinner or main tag
        }
        if (NOT_A_MEAL_CATEGORY.matcher(category).find()) {
            return null;
        }
        return MAIN_START; // no category, or one we don't recognise
    }

    private static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private static String lower(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private static String join(String... parts) {
        return Stream.of(parts).filter(Objects::nonNull).collect(Collectors.joining(" "));
    }
}