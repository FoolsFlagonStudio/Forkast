package com.forkast.backend.pricing;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.forkast.backend.ingredient.IngredientPrice;
import com.forkast.backend.ingredient.PriceSource;

/**
 * Picks one price per ingredient from its price history, trying each step in
 * order and
 * stopping at the first that has one:
 *
 * 1. Kroger, recorded within live-max-age (14 days)
 * 2. BLS, recorded within bls-max-age (60 days; it publishes monthly)
 * 3. The latest price from any source except the seed, however old; stale when
 * it's older
 * than live-max-age
 * 4. The seed price, a hand-entered estimate
 * 5. Nothing: the caller marks recipe cost incomplete
 *
 * Within a step, each product counts once, with its latest price, and the
 * cheapest wins. An
 * ingredient mapped to both loose onions and a 3 lb bag gets the bag's lower
 * price per 100 g:
 * the mappings are chosen by hand, so every one of them is a fair buy, and a
 * budget app should
 * assume the cheaper one.
 *
 * Rows without a unit price (a size that couldn't be read) are skipped. "now"
 * is passed in,
 * so tests don't depend on the clock.
 */
@Component
public class PriceResolver {

    private final Duration liveMaxAge;
    private final Duration blsMaxAge;

    public PriceResolver(PricingProperties properties) {
        this.liveMaxAge = properties.liveMaxAge();
        this.blsMaxAge = properties.blsMaxAge();
    }

    public Optional<ResolvedPrice> resolve(Collection<IngredientPrice> history, Instant now) {
        List<IngredientPrice> usable = history.stream()
                .filter(IngredientPrice::hasUnitPrice)
                .toList();
        if (usable.isEmpty()) {
            return Optional.empty();
        }
        Instant liveCutoff = now.minus(liveMaxAge);
        Instant blsCutoff = now.minus(blsMaxAge);

        // 1. Kroger, current
        Optional<ResolvedPrice> live = cheapest(usable,
                p -> p.getSource() == PriceSource.KROGER && !p.getRecordedAt().isBefore(liveCutoff), false);
        if (live.isPresent()) {
            return live;
        }

        // 2. BLS, current
        Optional<ResolvedPrice> bls = cheapest(usable,
                p -> p.getSource() == PriceSource.BLS && !p.getRecordedAt().isBefore(blsCutoff), false);
        if (bls.isPresent()) {
            return bls;
        }

        // 3. The latest real price, however old
        Optional<IngredientPrice> newest = usable.stream()
                .filter(p -> p.getSource() != PriceSource.SEED)
                .max(Comparator.comparing(IngredientPrice::getRecordedAt));
        if (newest.isPresent()) {
            PriceSource source = newest.get().getSource();
            boolean stale = newest.get().getRecordedAt().isBefore(liveCutoff);
            return cheapest(usable, p -> p.getSource() == source, stale);
        }

        // 4. The seed estimate
        return cheapest(usable, p -> p.getSource() == PriceSource.SEED, false);
    }

    /**
     * Among the prices that pass the filter: the latest per product, then the
     * lowest price per
     * 100 g and the lowest price per item. They can come from different products,
     * which is
     * fine: each is the cheapest way to buy the ingredient by that measure.
     */
    private static Optional<ResolvedPrice> cheapest(List<IngredientPrice> prices,
            Predicate<IngredientPrice> filter, boolean stale) {
        List<IngredientPrice> latestPerProduct = prices.stream()
                .filter(filter)
                .collect(Collectors.toMap(PriceResolver::productKey, Function.identity(),
                        (a, b) -> a.getRecordedAt().isAfter(b.getRecordedAt()) ? a : b))
                .values().stream()
                .toList();
        if (latestPerProduct.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal per100g = latestPerProduct.stream()
                .map(IngredientPrice::getPricePer100g)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        BigDecimal perItem = latestPerProduct.stream()
                .map(IngredientPrice::getPricePerItem)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        IngredientPrice newest = latestPerProduct.stream()
                .max(Comparator.comparing(IngredientPrice::getRecordedAt))
                .orElseThrow();

        return Optional.of(new ResolvedPrice(per100g, perItem, newest.getSource(), newest.getRecordedAt(), stale));
    }

    /**
     * Rows with no product id (seed, manual) all count as one product, so the
     * newest wins.
     */
    private static String productKey(IngredientPrice price) {
        return price.getSource() + ":" + (price.getExternalId() == null ? "" : price.getExternalId());
    }
}