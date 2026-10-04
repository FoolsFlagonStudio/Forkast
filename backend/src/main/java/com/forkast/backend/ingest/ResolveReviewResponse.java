package com.forkast.backend.ingest;

import java.util.UUID;

/**
 * alsoMatched counts other queued lines matched through the newly saved alias.
 */
public record ResolveReviewResponse(
        UUID id,
        String ingredientName,
        int alsoMatched) {
}