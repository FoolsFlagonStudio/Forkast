package com.forkast.backend.ingest;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One line in the review queue. Built directly by the JPQL query in
 * ReviewLineRepository.
 */
public record ReviewLineResponse(
        UUID id,
        UUID recipeId,
        String recipeName,
        String rawText,
        String parsedName,
        BigDecimal matchScore) {
}