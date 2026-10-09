package com.forkast.backend.pricing;

import java.math.BigDecimal;
import java.time.Instant;

import com.forkast.backend.ingredient.PriceSource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * One price to store, as import_prices.py sends it.
 *
 * @param ingredient          catalog name or alias ("onion", "yellow onion")
 * @param source              BLS, SEED, MANUAL or OPEN_PRICES; Kroger prices
 *                            come from the
 *                            refresh job instead
 * @param price               what the size costs, in USD
 * @param size                package size text, read by PackageSizeParser ("1
 *                            lb", "1 dozen")
 * @param externalId          the source's id, such as a BLS series id; optional
 * @param recordedAt          when the price was observed (a BLS month's end);
 *                            now if missing
 * @param averageGramsPerItem one item's weight, for a price per item; optional
 */
public record PriceImportRequest(
        @NotBlank @Size(max = 255) String ingredient,
        @NotNull PriceSource source,
        @NotNull @Positive BigDecimal price,
        @NotBlank @Size(max = 100) String size,
        @Size(max = 100) String externalId,
        Instant recordedAt,
        @Size(max = 255) String storeName,
        @Positive BigDecimal averageGramsPerItem) {
}