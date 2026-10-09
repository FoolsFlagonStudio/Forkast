package com.forkast.backend.pricing;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.forkast.backend.recipe.RecipeRepository;

/**
 * Recalculates every recipe's cost from current prices, after an import or a refresh. Prices
 * are resolved once for all ingredients, then recipes are updated 100 at a time, each batch in
 * its own transaction (RecipeCostBatch), so a long pass doesn't hold one huge transaction.
 */
@Service
public class RecipeCostService {

    private static final Logger log = LoggerFactory.getLogger(RecipeCostService.class);
    private static final int BATCH_SIZE = 100;

    private final RecipeRepository recipeRepository;
    private final CurrentPriceService currentPriceService;
    private final RecipeCostBatch batch;

    public RecipeCostService(RecipeRepository recipeRepository, CurrentPriceService currentPriceService,
            RecipeCostBatch batch) {
        this.recipeRepository = recipeRepository;
        this.currentPriceService = currentPriceService;
        this.batch = batch;
    }

    /** Returns how many recipes were updated. */
    public int recalculateAll() {
        Map<UUID, ResolvedPrice> prices = currentPriceService.allCurrentPrices();
        List<UUID> ids = recipeRepository.findAllIds();

        int updated = 0;
        for (int start = 0; start < ids.size(); start += BATCH_SIZE) {
            List<UUID> chunk = ids.subList(start, Math.min(start + BATCH_SIZE, ids.size()));
            updated += batch.update(chunk, prices);
        }
        log.info("Recalculated cost for {} recipes using {} priced ingredients", updated, prices.size());
        return updated;
    }
}