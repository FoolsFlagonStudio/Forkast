package com.forkast.backend.ingredient;

import java.math.BigDecimal;

/**
 * The matcher's answer for one parsed name. ingredient is null when nothing
 * scored high
 * enough; score is still kept (when there was any candidate) so the review
 * queue can show it.
 */
public record MatchResult(Ingredient ingredient, BigDecimal score) {

    public static MatchResult none() {
        return new MatchResult(null, null);
    }

    public boolean matched() {
        return ingredient != null;
    }
}