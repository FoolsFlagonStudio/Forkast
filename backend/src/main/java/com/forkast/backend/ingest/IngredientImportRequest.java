package com.forkast.backend.ingest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.forkast.backend.ingredient.IngredientTag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * One catalog ingredient, already extracted from FDC by seed_ingredients.py.
 */
public record IngredientImportRequest(
        @NotNull @Positive Long fdcId,
        @NotBlank @Size(max = 255) String name,
        List<@NotBlank @Size(max = 255) String> aliases,
        Set<@NotNull IngredientTag> tags,
        @PositiveOrZero BigDecimal caloriesPer100g,
        @PositiveOrZero BigDecimal proteinGPer100g,
        @PositiveOrZero BigDecimal carbsGPer100g,
        @PositiveOrZero BigDecimal fatGPer100g,
        List<@Valid @NotNull Portion> portions) {

    /** Weight of ONE of this portion, e.g. "cup, chopped" = 88 g. */
    public record Portion(
            @NotBlank @Size(max = 255) String description,
            @NotNull @Positive BigDecimal gramWeight) {
    }
}