package com.forkast.backend.ingest;

import java.util.List;

/** needsReview counts ingredient lines (across all created recipes) that didn't match confidently. */
public record RecipeIngestResult(
        int created,
        int skippedDuplicate,
        List<Failure> failed,
        int needsReview) {

    public record Failure(String sourceUrl, String reason) {
    }
}