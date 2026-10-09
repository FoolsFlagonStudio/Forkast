package com.forkast.backend.pricing;

import java.math.BigDecimal;

/**
 * A price in the units recipes need. Either can be null: an olive oil with no
 * density has
 * neither, eggs have both. Stored on IngredientPrice as price_per_100g and
 * price_per_item.
 */
public record UnitPrice(BigDecimal per100g, BigDecimal perItem) {

    public static final UnitPrice NONE = new UnitPrice(null, null);

    public boolean isKnown() {
        return per100g != null || perItem != null;
    }
}