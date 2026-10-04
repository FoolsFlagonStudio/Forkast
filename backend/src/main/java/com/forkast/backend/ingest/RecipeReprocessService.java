package com.forkast.backend.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Re-processes every saved recipe, one transaction per recipe (see
 * RecipeReprocessor).
 */
@Service
public class RecipeReprocessService {

    private static final Logger log = LoggerFactory.getLogger(RecipeReprocessService.class);

    private final ReviewLineRepository reviewLineRepository;
    private final RecipeReprocessor reprocessor;

    public RecipeReprocessService(ReviewLineRepository reviewLineRepository, RecipeReprocessor reprocessor) {
        this.reviewLineRepository = reviewLineRepository;
        this.reprocessor = reprocessor;
    }

    public ReprocessResult reprocessAll(boolean rematchAll) {
        int recipes = 0;
        int reparsed = 0;
        int newlyMatched = 0;
        int stillNeedsReview = 0;
        List<ReprocessResult.Failure> failed = new ArrayList<>();

        for (UUID recipeId : reviewLineRepository.findAllRecipeIds()) {
            try {
                RecipeReprocessor.Counts counts = reprocessor.reprocess(recipeId, rematchAll);
                recipes++;
                reparsed += counts.linesReparsed();
                newlyMatched += counts.newlyMatched();
                stillNeedsReview += counts.stillNeedsReview();
            } catch (RuntimeException ex) {
                log.warn("Reprocess failed for recipe {}: {}", recipeId, ex.getMessage());
                failed.add(new ReprocessResult.Failure(recipeId, ex.getMessage()));
            }
        }
        return new ReprocessResult(recipes, reparsed, newlyMatched, stillNeedsReview, failed);
    }
}