package com.forkast.backend.pricing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientProduct;
import com.forkast.backend.ingredient.IngredientProductRepository;

/**
 * Saves which store products stand in for which ingredients (kroger_products.csv). Matches on
 * ingredient + source + product id, so rerunning updates labels and active flags instead of
 * adding duplicates. Rows missing from the file are left alone.
 */
@Service
public class ProductMappingService {

    private final IngredientLookup ingredientLookup;
    private final IngredientProductRepository productRepository;

    public ProductMappingService(IngredientLookup ingredientLookup, IngredientProductRepository productRepository) {
        this.ingredientLookup = ingredientLookup;
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductMappingResult importAll(List<ProductMappingRequest> requests) {
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        List<PriceImportResult.Failure> failed = new ArrayList<>();

        for (ProductMappingRequest request : requests) {
            Optional<Ingredient> ingredient = ingredientLookup.find(request.ingredient());
            if (ingredient.isEmpty()) {
                failed.add(new PriceImportResult.Failure(request.ingredient(), "No ingredient with that name or alias"));
                continue;
            }
            String externalId = request.externalId().trim();
            boolean active = request.active() == null || request.active();

            Optional<IngredientProduct> existing = productRepository
                    .findByIngredientIdAndSourceAndExternalId(ingredient.get().getId(), request.source(), externalId);
            if (existing.isEmpty()) {
                IngredientProduct product = new IngredientProduct(ingredient.get(), request.source(), externalId,
                        request.label());
                if (!active) {
                    product.deactivate();
                }
                productRepository.save(product);
                created++;
                continue;
            }

            IngredientProduct product = existing.get();
            boolean changed = false;
            if (request.label() != null && !Objects.equals(product.getLabel(), request.label().trim())) {
                product.setLabel(request.label());
                changed = true;
            }
            if (product.isActive() != active) {
                if (active) {
                    product.activate();
                } else {
                    product.deactivate();
                }
                changed = true;
            }
            if (changed) {
                updated++;
            } else {
                unchanged++;
            }
        }
        return new ProductMappingResult(created, updated, unchanged, failed);
    }
}