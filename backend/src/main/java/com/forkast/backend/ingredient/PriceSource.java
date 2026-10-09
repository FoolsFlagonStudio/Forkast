package com.forkast.backend.ingredient;

/** Where a price observation came from. */
public enum PriceSource {
    /** Open Food Facts' crowd-sourced prices, by barcode */
    OPEN_PRICES,
    /** Entered by a person */
    MANUAL,
    /** Kroger's product API, for one representative store */
    KROGER,
    /** US Bureau of Labor Statistics average retail prices, monthly */
    BLS,
    /** Hand-entered typical prices from the seed CSV, the last resort */
    SEED
}