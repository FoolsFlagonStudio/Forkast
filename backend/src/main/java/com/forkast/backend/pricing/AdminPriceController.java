package com.forkast.backend.pricing;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forkast.backend.pricing.kroger.KrogerRefreshResult;
import com.forkast.backend.pricing.kroger.KrogerRefreshService;

import jakarta.validation.Valid;

/** Admin-only (X-Admin-Key), like the other /api/admin routes. */
@RestController
@RequestMapping("/api/admin/prices")
public class AdminPriceController {

    private final PriceImportService importService;
    private final RecipeCostService costService;
    private final ProductMappingService mappingService;
    private final KrogerRefreshService krogerRefreshService;

    public AdminPriceController(PriceImportService importService, RecipeCostService costService,
            ProductMappingService mappingService, KrogerRefreshService krogerRefreshService) {
        this.importService = importService;
        this.costService = costService;
        this.mappingService = mappingService;
        this.krogerRefreshService = krogerRefreshService;
    }

    /**
     * Stores the batch, then recalculates every recipe's cost if anything new came
     * in. Always
     * 200 for a well-formed batch; per-price problems are listed in "failed".
     */
    @PostMapping("/import")
    public PriceImportResult importPrices(@Valid @RequestBody PriceImportBatch batch) {
        PriceImportResult result = importService.importAll(batch.prices());
        if (result.imported() == 0) {
            return result;
        }
        return result.withRecipesRecosted(costService.recalculateAll());
    }

    /**
     * Recalculates every recipe's cost from current prices without importing
     * anything.
     */
    @PostMapping("/recalculate")
    public Map<String, Integer> recalculate() {
        return Map.of("recipesRecosted", costService.recalculateAll());
    }

    /**
     * Saves which store products stand in for which ingredients
     * (kroger_products.csv).
     */
    @PostMapping("/products")
    public ProductMappingResult importProducts(@Valid @RequestBody ProductMappingBatch batch) {
        return mappingService.importAll(batch.products());
    }

    /**
     * Re-prices every active Kroger product now, then recalculates recipe costs.
     * Takes about a
     * quarter second per product.
     */
    @PostMapping("/refresh")
    public KrogerRefreshResult refreshKroger() {
        return krogerRefreshService.refresh();
    }
}