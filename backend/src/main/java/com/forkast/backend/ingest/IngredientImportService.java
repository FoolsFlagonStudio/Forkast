package com.forkast.backend.ingest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.common.exception.ApiException;
import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientAliasRepository;
import com.forkast.backend.ingredient.IngredientPortion;
import com.forkast.backend.ingredient.IngredientRepository;

@Service
public class IngredientImportService {

    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;

    public IngredientImportService(IngredientRepository ingredientRepository,
            IngredientAliasRepository aliasRepository) {
        this.ingredientRepository = ingredientRepository;
        this.aliasRepository = aliasRepository;
    }

    /**
     * Creates or updates each ingredient by fdcId. One bad item rolls back the
     * whole batch.
     */
    @Transactional
    public IngredientImportResult importAll(List<IngredientImportRequest> items) {
        int created = 0;
        int updated = 0;

        for (IngredientImportRequest item : items) {
            Optional<Ingredient> existing = ingredientRepository.findByFdcId(item.fdcId());
            Ingredient ingredient;

            if (existing.isPresent()) {
                ingredient = existing.get();
                ingredient.setName(item.name());
                // Delete the old portions now. Hibernate runs INSERTs before DELETEs in a
                // flush,
                // so re-adding "cup" before the old "cup" is gone would break the unique
                // constraint.
                ingredient.replacePortions(List.of());
                ingredientRepository.flush();
                updated++;
            } else {
                ingredient = new Ingredient(item.name());
                ingredient.setFdcId(item.fdcId());
                created++;
            }

            ingredient.setCaloriesPer100g(scale(item.caloriesPer100g()));
            ingredient.setProteinGPer100g(scale(item.proteinGPer100g()));
            ingredient.setCarbsGPer100g(scale(item.carbsGPer100g()));
            ingredient.setFatGPer100g(scale(item.fatGPer100g()));
            ingredient.replaceTags(item.tags());
            ingredient.replacePortions(toPortions(item.portions()));
            addAliases(ingredient, item.aliases());

            ingredientRepository.save(ingredient);
        }

        return new IngredientImportResult(created, updated);
    }

    // ---------- helpers ----------

    /**
     * Adds aliases but never removes them, so aliases saved later from the review
     * queue
     * survive a re-import. An alias owned by a different ingredient is a seed file
     * mistake.
     */
    private void addAliases(Ingredient ingredient, List<String> aliases) {
        if (aliases == null) {
            return;
        }
        for (String alias : aliases) {
            String normalized = alias.trim().toLowerCase();
            aliasRepository.findByAlias(normalized).ifPresent(owned -> {
                if (!Objects.equals(owned.getIngredient().getId(), ingredient.getId())) {
                    throw ApiException.conflict(
                            "Alias '" + normalized + "' already belongs to another ingredient.");
                }
            });
            ingredient.addAlias(normalized);
        }
    }

    /** Builds portion entities, keeping the first of any duplicate descriptions. */
    private List<IngredientPortion> toPortions(List<IngredientImportRequest.Portion> portions) {
        if (portions == null) {
            return List.of();
        }
        Map<String, IngredientPortion> byDescription = new LinkedHashMap<>();
        for (IngredientImportRequest.Portion p : portions) {
            byDescription.putIfAbsent(p.description().trim().toLowerCase(),
                    new IngredientPortion(p.description(), scale(p.gramWeight())));
        }
        return List.copyOf(byDescription.values());
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}