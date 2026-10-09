package com.forkast.backend.pricing.kroger;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The parts of a Kroger product we use. Kroger sends far more (images, aisles,
 * nutrition);
 * ignoreUnknown skips the rest. Field names match Kroger's JSON exactly, as
 * seen in the step 1
 * test run.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KrogerProduct(
        String productId,
        String description,
        List<String> categories,
        List<Item> items,
        ItemInformation itemInformation) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String itemId, Price price, String size, String soldBy) {
    }

    /**
     * regular is always there when the store has a price; promo only during a sale.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Price(BigDecimal regular, BigDecimal promo) {
    }

    /**
     * Weights look like "0.5 [lb_av]": the weight of one sold unit (a loose onion,
     * a bunch)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemInformation(String averageWeightPerUnit, String netWeight) {
    }

    /** GET /v1/products/{id} wraps the product in "data" */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(KrogerProduct data) {
    }
}