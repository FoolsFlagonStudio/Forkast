package com.forkast.backend.pricing.kroger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.forkast.backend.common.exception.ApiException;
import com.forkast.backend.ingredient.IngredientProduct;
import com.forkast.backend.ingredient.IngredientProductRepository;
import com.forkast.backend.ingredient.PriceSource;
import com.forkast.backend.pricing.RecipeCostService;

/**
 * Re-prices every active Kroger mapping at the configured store, one product
 * per request,
 * then recalculates recipe costs. Runs weekly (KrogerRefreshScheduler) and on
 * demand
 * (POST /api/admin/prices/refresh).
 *
 * A product that fails is logged and skipped; nothing is deleted, so its last
 * price stays in
 * use through the resolver's fallback chain. Only one refresh runs at a time.
 */
@Service
public class KrogerRefreshService {

    private static final Logger log = LoggerFactory.getLogger(KrogerRefreshService.class);

    private final KrogerClient client;
    private final KrogerProperties properties;
    private final KrogerPriceWriter writer;
    private final IngredientProductRepository productRepository;
    private final RecipeCostService recipeCostService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public KrogerRefreshService(KrogerClient client, KrogerProperties properties, KrogerPriceWriter writer,
            IngredientProductRepository productRepository, RecipeCostService recipeCostService) {
        this.client = client;
        this.properties = properties;
        this.writer = writer;
        this.productRepository = productRepository;
        this.recipeCostService = recipeCostService;
    }

    public KrogerRefreshResult refresh() {
        if (!client.configured()) {
            throw ApiException.badRequest(
                    "Kroger isn't configured: set forkast.pricing.kroger.client-id and client-secret.");
        }
        if (!running.compareAndSet(false, true)) {
            throw ApiException.conflict("A Kroger refresh is already running.");
        }
        try {
            return refreshAll();
        } finally {
            running.set(false);
        }
    }

    private KrogerRefreshResult refreshAll() {
        List<IngredientProduct> mappings = productRepository.findActiveWithIngredient(PriceSource.KROGER);
        String storeName = "Kroger " + client.locationId();
        int priced = 0;
        int noUnitPrice = 0;
        List<KrogerRefreshResult.Failure> failed = new ArrayList<>();

        log.info("Kroger refresh: {} products at store {}", mappings.size(), client.locationId());
        for (IngredientProduct mapping : mappings) {
            String ingredient = mapping.getIngredient().getName();
            String productId = mapping.getExternalId();
            try {
                Optional<KrogerProduct> product = client.product(productId);
                Optional<KrogerQuote> quote = product.flatMap(KrogerQuote::from);
                if (product.isEmpty()) {
                    failed.add(new KrogerRefreshResult.Failure(ingredient, productId,
                            "Kroger has no product with this id"));
                } else if (quote.isEmpty()) {
                    failed.add(new KrogerRefreshResult.Failure(ingredient, productId, "No price at this store"));
                } else if (writer.save(mapping.getId(), quote.get(), storeName)) {
                    priced++;
                } else {
                    noUnitPrice++;
                    log.info("Kroger {} ({}): stored, but can't convert size '{}'", productId, ingredient,
                            quote.get().size());
                }
            } catch (RestClientException e) {
                failed.add(new KrogerRefreshResult.Failure(ingredient, productId, "Request failed: " + e.getMessage()));
                log.warn("Kroger {} ({}) failed: {}", productId, ingredient, e.getMessage());
            }
            pause();
        }

        if (!mappings.isEmpty() && failed.size() > mappings.size() / 2) {
            log.error("Kroger refresh: {} of {} products failed. The API or credentials may have changed.",
                    failed.size(), mappings.size());
        }
        int recosted = priced > 0 ? recipeCostService.recalculateAll() : 0;
        log.info("Kroger refresh done: {} priced, {} without unit price, {} failed, {} recipes recosted",
                priced, noUnitPrice, failed.size(), recosted);
        return new KrogerRefreshResult(mappings.size(), priced, noUnitPrice, failed, recosted);
    }

    /**
     * Spaces requests out so a weekly run stays well inside Kroger's rate limit.
     */
    private void pause() {
        try {
            Thread.sleep(properties.requestDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}