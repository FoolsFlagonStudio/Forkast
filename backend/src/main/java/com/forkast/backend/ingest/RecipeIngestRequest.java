package com.forkast.backend.ingest;

import java.util.List;

/**
 * One scraped recipe, raw, exactly as scrape_recipes.py sends it. Missing
 * fields are null.
 *
 * There are deliberately no validation annotations here: a bad recipe should
 * land in the
 * response's failed list (RecipeImporter checks it) instead of rejecting the
 * whole batch
 * with a 400.
 */
public record RecipeIngestRequest(
        String sourceUrl,
        String host,
        String name,
        String description,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes,
        String yields,
        List<String> rawIngredients,
        List<String> rawInstructions,
        String imageUrl,
        String category,
        String cuisine,
        List<String> keywords) {
}