package com.forkast.backend.pricing;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.ingest.RecipeEnricher;
import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.recipe.RecipeRepository;

/**
 * One batch of RecipeCostService's pass. A separate bean because @Transactional
 * works through
 * Spring's proxy, which a class calling its own method skips (same reason as
 * RecipeImporter).
 */
@Service
public class RecipeCostBatch {

    private final RecipeRepository recipeRepository;
    private final RecipeEnricher enricher;

    public RecipeCostBatch(RecipeRepository recipeRepository, RecipeEnricher enricher) {
        this.recipeRepository = recipeRepository;
        this.enricher = enricher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int update(List<UUID> recipeIds, Map<UUID, ResolvedPrice> prices) {
        List<Recipe> recipes = recipeRepository.findAllById(recipeIds);
        recipes.forEach(recipe -> enricher.updateCost(recipe, prices));
        return recipes.size();
    }
}