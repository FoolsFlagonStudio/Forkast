package com.forkast.backend.ingredient;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns a parsed ingredient name into the lookups the matcher should try, most
 * specific first:
 * the name as written, its singular, then the name with descriptor words
 * removed, and that
 * name's singular.
 *
 * "Boneless Skinless Chicken Breasts" ->
 * boneless skinless chicken breasts, boneless skinless chicken breast,
 * chicken breasts, chicken breast
 */
public final class IngredientNames {

    /**
     * Words that describe preparation or quality rather than what the ingredient
     * is.
     * "ground" is deliberately missing: "ground beef" and "ground ginger" are
     * different
     * ingredients from "beef" and "ginger". Exact matches on the full name are
     * tried
     * before any of these are removed, so "crushed tomatoes" still matches itself.
     */
    private static final Set<String> DESCRIPTORS = Set.of(
            "fresh", "freshly", "frozen", "dried", "canned", "raw", "cooked", "uncooked",
            "chopped", "diced", "minced", "sliced", "shredded", "grated", "crushed", "cubed",
            "halved", "quartered", "julienned", "peeled", "seeded", "trimmed", "rinsed", "drained",
            "melted", "softened", "packed", "sifted", "beaten", "toasted", "cracked",
            "reserved", "reserve", "heaping", "scant",
            "finely", "roughly", "coarsely", "thinly", "lightly", "very",
            "boneless", "skinless", "lean", "extra-lean", "organic", "whole",
            "large", "medium", "small", "extra-large", "jumbo");

    private IngredientNames() {
        // static helpers only
    }

    public static List<String> candidates(String parsedName) {
        String base = normalize(parsedName);
        if (base.isEmpty()) {
            return List.of();
        }

        Set<String> candidates = new LinkedHashSet<>(); // keeps insertion order, drops duplicates
        candidates.add(base);
        candidates.add(singular(base));

        String stripped = stripDescriptors(base);
        if (!stripped.isEmpty()) {
            candidates.add(stripped);
            candidates.add(singular(stripped));
        }
        return List.copyOf(candidates);
    }

    /**
     * True for prep and quality words like "chopped" or "boneless". The parser uses
     * this too.
     */
    public static boolean isDescriptor(String word) {
        return word != null && DESCRIPTORS.contains(word.toLowerCase(Locale.ROOT));
    }

    /**
     * Lowercase, keep letters, digits, %, hyphens and apostrophes, collapse spaces.
     */
    static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9%'\\- ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Drops descriptor words and anything with a digit in it ("93%", "1-inch"). */
    static String stripDescriptors(String name) {
        return Arrays.stream(name.split(" "))
                .filter(word -> !DESCRIPTORS.contains(word))
                .filter(word -> !word.matches(".*\\d.*"))
                .collect(Collectors.joining(" "));
    }

    /**
     * Naive English singular of the last word only: "cherry tomatoes" -> "cherry
     * tomato".
     */
    static String singular(String name) {
        int lastSpace = name.lastIndexOf(' ');
        String head = name.substring(0, lastSpace + 1);
        String word = name.substring(lastSpace + 1);

        if (word.endsWith("ies") && word.length() > 4) {
            word = word.substring(0, word.length() - 3) + "y"; // berries -> berry
        } else if (word.endsWith("oes") || word.endsWith("ches") || word.endsWith("shes")
                || word.endsWith("sses") || word.endsWith("xes")) {
            word = word.substring(0, word.length() - 2); // tomatoes -> tomato
        } else if (word.endsWith("s") && !word.endsWith("ss") && word.length() > 3) {
            word = word.substring(0, word.length() - 1); // carrots -> carrot
        }
        return head + word;
    }
}