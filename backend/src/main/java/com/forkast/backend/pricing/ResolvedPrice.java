package com.forkast.backend.pricing;

import java.math.BigDecimal;
import java.time.Instant;

import com.forkast.backend.ingredient.PriceSource;

/**
 * The one price used for an ingredient right now, and where it came from. stale
 * means it's
 * the best there is but older than the live window, so the app can say
 * "estimate".
 */
public record ResolvedPrice(
        BigDecimal per100g,
        BigDecimal perItem,
        PriceSource source,
        Instant recordedAt,
        boolean stale) {
}