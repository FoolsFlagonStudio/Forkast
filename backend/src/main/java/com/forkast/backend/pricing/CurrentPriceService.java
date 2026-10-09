package com.forkast.backend.pricing;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.ingredient.IngredientPrice;
import com.forkast.backend.ingredient.IngredientPriceRepository;
import com.forkast.backend.ingredient.IngredientProduct;
import com.forkast.backend.ingredient.IngredientProductRepository;

/**
 * Loads price history for a set of ingredients in one query and resolves each
 * to its current
 * price. The enricher asks for one recipe's ingredients; the cost pass after a
 * refresh will
 * ask for all of them at once.
 */
@Service
public class CurrentPriceService {

    private final IngredientPriceRepository priceRepository;
    private final IngredientProductRepository productRepository;
    private final PriceResolver resolver;

    public CurrentPriceService(IngredientPriceRepository priceRepository,
            IngredientProductRepository productRepository, PriceResolver resolver) {
        this.priceRepository = priceRepository;
        this.productRepository = productRepository;
        this.resolver = resolver;
    }

    /** Ingredients with no usable price are missing from the map. */
    @Transactional(readOnly = true)
    public Map<UUID, ResolvedPrice> currentPrices(Collection<UUID> ingredientIds) {
        if (ingredientIds.isEmpty()) {
            return Map.of();
        }
        return resolve(priceRepository.findByIngredientIds(ingredientIds));
    }

    /** Every ingredient's current price, for recalculating every recipe at once. */
    @Transactional(readOnly = true)
    public Map<UUID, ResolvedPrice> allCurrentPrices() {
        return resolve(priceRepository.findAll());
    }

    /**
     * A product that was switched off (replaced by a better pick, say) keeps its
     * price history,
     * but those prices no longer count. Otherwise a wrong pick that happened to be
     * cheap, like
     * lemon soda for lemons, would keep winning "cheapest product" until it aged
     * out.
     */
    private Map<UUID, ResolvedPrice> resolve(List<IngredientPrice> history) {
        Set<String> switchedOff = productRepository.findByActiveFalse().stream()
                .map(p -> key(p.getIngredient().getId(), p.getSource(), p.getExternalId()))
                .collect(Collectors.toSet());

        Instant now = Instant.now();
        Map<UUID, List<IngredientPrice>> byIngredient = history.stream()
                .filter(p -> p.getExternalId() == null
                        || !switchedOff.contains(key(p.getIngredient().getId(), p.getSource(), p.getExternalId())))
                .collect(Collectors.groupingBy(p -> p.getIngredient().getId()));

        return byIngredient.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), resolver.resolve(e.getValue(), now)))
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    private static String key(UUID ingredientId, Object source, String externalId) {
        return ingredientId + ":" + source + ":" + externalId;
    }
}