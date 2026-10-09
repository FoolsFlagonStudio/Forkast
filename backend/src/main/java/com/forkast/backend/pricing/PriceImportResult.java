package com.forkast.backend.pricing;

import java.util.List;

/**
 * skipped: already stored (the same BLS month, or a seed price that didn't
 * change).
 * recipesRecosted: how many recipes the cost pass updated afterwards; 0 when
 * nothing new was
 * imported.
 */
public record PriceImportResult(int imported, int skipped, List<Failure> failed, int recipesRecosted) {

    public record Failure(String ingredient, String reason) {
    }

    PriceImportResult withRecipesRecosted(int count) {
        return new PriceImportResult(imported, skipped, failed, count);
    }
}