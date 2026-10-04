package com.forkast.backend.ingest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a serving count out of a raw "yields" string: "6 servings", "Makes 12
 * muffins", "4-6".
 */
public final class Servings {

    private static final Pattern FIRST_NUMBER = Pattern.compile("\\d{1,4}");

    private Servings() {
    }

    /** The first number in the text, or null when there is none (or it's 0). */
    public static Integer parse(String yields) {
        if (yields == null) {
            return null;
        }
        Matcher number = FIRST_NUMBER.matcher(yields);
        if (!number.find()) {
            return null;
        }
        int servings = Integer.parseInt(number.group());
        return servings >= 1 ? servings : null;
    }
}