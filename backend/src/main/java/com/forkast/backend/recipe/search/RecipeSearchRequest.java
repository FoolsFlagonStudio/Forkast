package com.forkast.backend.recipe.search;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * The query parameters of GET /api/recipes. Spring builds this record from the
 * URL
 * (?q=chili&labels=vegan,high-protein&sort=protein); every field is optional.
 */
public record RecipeSearchRequest(
        @Size(max = 100) String q,
        List<String> labels,
        @PositiveOrZero BigDecimal minProtein,
        @Positive BigDecimal maxCalories,
        @Positive Integer maxTime,
        String sort,
        @Min(0) Integer page,
        @Min(1) @Max(50) Integer size) {

    public static final int DEFAULT_SIZE = 20;

    public int pageOrDefault() {
        return page == null ? 0 : page;
    }

    public int sizeOrDefault() {
        return size == null ? DEFAULT_SIZE : size;
    }
}