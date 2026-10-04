package com.forkast.backend.ingest;

import java.math.BigDecimal;

/**
 * One ingredient line split into parts. amount and unit are null when the line
 * has no
 * quantity ("cooking spray"); skip is true for lines that aren't ingredients
 * ("For the sauce:").
 */
public record ParsedLine(
        BigDecimal amount,
        Unit unit,
        String name,
        String prepNote,
        boolean optional,
        boolean skip) {

    public static ParsedLine skipped() {
        return new ParsedLine(null, null, null, null, false, true);
    }
}