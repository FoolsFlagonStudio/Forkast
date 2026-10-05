package com.forkast.backend.common;

import java.util.List;

/**
 * A plain page for JSON responses, instead of serializing Spring's Page
 * directly.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long total) {
}