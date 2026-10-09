package com.forkast.backend.pricing;

import com.forkast.backend.ingredient.PriceSource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One row of kroger_products.csv: this store product stands in for this ingredient.
 * active defaults to true; send false to stop refreshing a product without losing its history.
 */
public record ProductMappingRequest(
        @NotBlank @Size(max = 255) String ingredient,
        @NotNull PriceSource source,
        @NotBlank @Size(max = 100) String externalId,
        @Size(max = 255) String label,
        Boolean active) {
}