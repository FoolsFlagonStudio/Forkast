package com.forkast.backend.mealplan.grocery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientPrice;
import com.forkast.backend.ingredient.IngredientPriceRepository;
import com.forkast.backend.ingredient.IngredientProduct;
import com.forkast.backend.ingredient.IngredientProductRepository;
import com.forkast.backend.ingredient.PriceSource;
import com.forkast.backend.ingredient.SoldBy;
import com.forkast.backend.pricing.CurrentPriceService;
import com.forkast.backend.pricing.ResolvedPrice;

/**
 * Loads the two kinds of price a grocery list needs, for a set of ingredients
 * in a few
 * queries: the resolved price (for usage cost, the same one recipe cost uses)
 * and the
 * packages each ingredient can be bought in (its active Kroger products, latest
 * price each).
 *
 * A package's weight comes from its own row: price / price per 100 g. That's
 * the size Kroger
 * gave, already converted (by density for liquids) when the price was stored.
 */
@Component
public class GroceryPriceLoader {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CurrentPriceService currentPriceService;
    private final IngredientPriceRepository priceRepository;
    private final IngredientProductRepository productRepository;

    public GroceryPriceLoader(CurrentPriceService currentPriceService, IngredientPriceRepository priceRepository,
            IngredientProductRepository productRepository) {
        this.currentPriceService = currentPriceService;
        this.priceRepository = priceRepository;
        this.productRepository = productRepository;
    }

    public GroceryListBuilder.Prices load(Collection<UUID> ingredientIds) {
        Map<UUID, ResolvedPrice> resolved = currentPriceService.currentPrices(ingredientIds);
        Map<UUID, List<GroceryListBuilder.PackageOption>> packages = packages(ingredientIds);
        return new GroceryListBuilder.Prices() {
            @Override
            public Optional<ResolvedPrice> resolved(Ingredient ingredient) {
                return Optional.ofNullable(resolved.get(ingredient.getId()));
            }

            @Override
            public List<GroceryListBuilder.PackageOption> packages(Ingredient ingredient) {
                return packages.getOrDefault(ingredient.getId(), List.of());
            }
        };
    }

    private Map<UUID, List<GroceryListBuilder.PackageOption>> packages(Collection<UUID> ingredientIds) {
        if (ingredientIds.isEmpty()) {
            return Map.of();
        }
        Map<String, IngredientProduct> active = productRepository.findActiveWithIngredient(PriceSource.KROGER).stream()
                .collect(Collectors.toMap(p -> key(p.getIngredient().getId(), p.getExternalId()), Function.identity(),
                        (a, b) -> a));

        // The latest price of each active Kroger product
        Map<String, IngredientPrice> latest = priceRepository.findByIngredientIds(ingredientIds).stream()
                .filter(p -> p.getSource() == PriceSource.KROGER && p.getExternalId() != null)
                .filter(p -> active.containsKey(key(p.getIngredient().getId(), p.getExternalId())))
                .collect(Collectors.toMap(p -> key(p.getIngredient().getId(), p.getExternalId()), Function.identity(),
                        (a, b) -> a.getRecordedAt().isAfter(b.getRecordedAt()) ? a : b));

        return latest.entrySet().stream()
                .map(e -> Map.entry(e.getValue().getIngredient().getId(), option(e.getValue(), active.get(e.getKey()))))
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(e -> e.getValue().get(), Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(GroceryListBuilder.PackageOption::label,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                                        .toList()))));
    }

    /**
     * A price row as a way to buy the ingredient; empty when there's nothing to
     * size it by.
     */
    static Optional<GroceryListBuilder.PackageOption> option(IngredientPrice row, IngredientProduct product) {
        String label = label(product == null ? null : product.getLabel(), row.getSizeText());
        if (row.getSoldBy() == SoldBy.WEIGHT) {
            return Optional.of(new GroceryListBuilder.PackageOption(label + " (by the lb)", row.getPrice(), null,
                    null, true));
        }
        BigDecimal grams = row.getPricePer100g() == null || row.getPricePer100g().signum() == 0 ? null
                : row.getPrice().multiply(HUNDRED).divide(row.getPricePer100g(), 3, RoundingMode.HALF_UP);
        BigDecimal items = "count".equals(row.getUnit()) ? row.getQuantity() : null;
        if (grams == null && items == null) {
            return Optional.empty();
        }
        return Optional.of(new GroceryListBuilder.PackageOption(label, row.getPrice(), grams, items, false));
    }

    private static String label(String productLabel, String sizeText) {
        if (productLabel == null || productLabel.isBlank()) {
            return sizeText;
        }
        return sizeText == null || sizeText.isBlank() ? productLabel : productLabel + ", " + sizeText;
    }

    private static String key(UUID ingredientId, String externalId) {
        return ingredientId + ":" + externalId;
    }
}