package com.forkast.backend.pricing.kroger;

import java.util.List;

/**
 * products: active mappings tried. priced: stored with a usable unit price.
 * noUnitPrice:
 * stored, but the size couldn't be converted. failed: nothing stored (not
 * found, no price at
 * the store, or the call failed).
 */
public record KrogerRefreshResult(int products, int priced, int noUnitPrice, List<Failure> failed,
        int recipesRecosted) {

    public record Failure(String ingredient, String productId, String reason) {
    }
}