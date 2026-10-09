package com.forkast.backend.ingredient;

/**
 * How a store prices a product. WEIGHT: the price is per pound and the bill
 * depends on what's
 * weighed (loose onions, chicken trays). UNIT: the price is for one package of
 * the stated size
 * (a 3 lb bag of onions).
 */
public enum SoldBy {
    UNIT,
    WEIGHT
}