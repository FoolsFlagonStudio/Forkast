package com.forkast.backend.ingest;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.common.exception.ApiException;
import com.forkast.backend.ingredient.IngredientMatcher;
import com.forkast.backend.ingredient.MatchResult;
import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.recipe.RecipeIngredient;
import com.forkast.backend.recipe.RecipeRepository;

/**
 * Re-runs the parser and matcher on one saved recipe from its stored raw text,
 * then
 * recomputes nutrition, labels and the meal-prep score. Its own transaction,
 * like
 * RecipeImporter, so one bad recipe doesn't stop the rest.
 *
 * Every line is re-parsed. Only unmatched lines are re-matched, unless
 * rematchAll is set:
 * a line matched by hand without saving an alias would otherwise lose its
 * match.
 */
@Service
public class RecipeReprocessor {

    public record Counts(int linesReparsed, int newlyMatched, int stillNeedsReview) {
    }

    private final RecipeRepository recipeRepository;
    private final IngredientLineParser parser;
    private final IngredientMatcher matcher;
    private final RecipeEnricher enricher;

    public RecipeReprocessor(RecipeRepository recipeRepository,
            IngredientLineParser parser,
            IngredientMatcher matcher,
            RecipeEnricher enricher) {
        this.recipeRepository = recipeRepository;
        this.parser = parser;
        this.matcher = matcher;
        this.enricher = enricher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Counts reprocess(UUID recipeId, boolean rematchAll) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> ApiException.notFound("Recipe not found."));

        int reparsed = 0;
        int newlyMatched = 0;
        int stillNeedsReview = 0;

        for (RecipeIngredient line : recipe.getIngredients()) {
            ParsedLine parsed = parser.parse(line.getRawText());
            if (parsed.skip()) {
                continue; // now reads as a header; leave the stored line alone
            }
            RecipeImporter.applyParse(line, parsed);
            reparsed++;

            if (line.isNeedsReview() || rematchAll) {
                boolean wasUnmatched = line.getIngredient() == null;
                MatchResult match = matcher.match(parsed.name());
                if (match.matched()) {
                    line.matchIngredient(match.ingredient(), match.score());
                    if (wasUnmatched) {
                        newlyMatched++;
                    }
                } else {
                    line.flagForReview(match.score());
                }
            }
            if (line.isNeedsReview()) {
                stillNeedsReview++;
            }
        }

        enricher.enrich(recipe);
        return new Counts(reparsed, newlyMatched, stillNeedsReview);
    }
}