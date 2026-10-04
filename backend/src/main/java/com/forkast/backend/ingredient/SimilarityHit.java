package com.forkast.backend.ingredient;

import java.util.UUID;

/**
 * Projection for IngredientRepository.findMostSimilar: one row, two columns.
 */
public interface SimilarityHit {

    UUID getIngredientId();

    Double getScore();
}