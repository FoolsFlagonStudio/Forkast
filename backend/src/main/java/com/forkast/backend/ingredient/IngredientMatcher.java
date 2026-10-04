package com.forkast.backend.ingredient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds the catalog ingredient for a parsed name.
 *
 * 1. Exact match on an ingredient name or alias, trying each candidate from
 * IngredientNames
 * (as written, singular, descriptors removed). Score 1.000.
 * 2. Otherwise the best trigram similarity across all names and aliases. 0.5 or
 * higher is
 * a match; lower returns no ingredient but keeps the score for the review
 * queue.
 */
@Component
public class IngredientMatcher {

    static final BigDecimal THRESHOLD = new BigDecimal("0.500");
    private static final BigDecimal EXACT = new BigDecimal("1.000");

    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;

    public IngredientMatcher(IngredientRepository ingredientRepository,
            IngredientAliasRepository aliasRepository) {
        this.ingredientRepository = ingredientRepository;
        this.aliasRepository = aliasRepository;
    }

    @Transactional(readOnly = true)
    public MatchResult match(String parsedName) {
        List<String> candidates = IngredientNames.candidates(parsedName);
        if (candidates.isEmpty()) {
            return MatchResult.none();
        }

        for (String candidate : candidates) {
            Optional<Ingredient> exact = findExact(candidate);
            if (exact.isPresent()) {
                return new MatchResult(exact.get(), EXACT);
            }
        }

        UUID bestId = null;
        BigDecimal bestScore = null;
        for (String candidate : candidates) {
            Optional<SimilarityHit> hit = ingredientRepository.findMostSimilar(candidate);
            if (hit.isEmpty() || hit.get().getScore() == null) {
                continue;
            }
            BigDecimal score = BigDecimal.valueOf(hit.get().getScore()).setScale(3, RoundingMode.HALF_UP);
            if (bestScore == null || score.compareTo(bestScore) > 0) {
                bestScore = score;
                bestId = hit.get().getIngredientId();
            }
        }

        if (bestId != null && bestScore.compareTo(THRESHOLD) >= 0) {
            return new MatchResult(ingredientRepository.findById(bestId).orElseThrow(), bestScore);
        }
        return new MatchResult(null, bestScore);
    }

    private Optional<Ingredient> findExact(String candidate) {
        return ingredientRepository.findByName(candidate)
                .or(() -> aliasRepository.findByAlias(candidate).map(IngredientAlias::getIngredient));
    }
}