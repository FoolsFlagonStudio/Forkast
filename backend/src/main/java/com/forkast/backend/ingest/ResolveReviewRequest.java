package com.forkast.backend.ingest;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Manually match a line. With saveAlias, the line's parsed name is saved as an
 * alias of the
 * ingredient, so the same text matches automatically from now on, and other
 * lines waiting
 * in the queue with that name are matched right away.
 */
public record ResolveReviewRequest(
        @NotNull UUID ingredientId,
        Boolean saveAlias) {
}