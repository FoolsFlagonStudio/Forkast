package com.forkast.backend.ingest;

import java.util.List;

/**
 * A plain page shape for JSON, instead of serializing Spring's Page directly.
 */
public record ReviewPage(
        List<ReviewLineResponse> items,
        int page,
        int size,
        long total) {
}