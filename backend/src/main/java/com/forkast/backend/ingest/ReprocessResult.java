package com.forkast.backend.ingest;

import java.util.List;
import java.util.UUID;

public record ReprocessResult(
        int recipes,
        int linesReparsed,
        int newlyMatched,
        int stillNeedsReview,
        List<Failure> failed) {

    public record Failure(UUID recipeId, String reason) {
    }
}