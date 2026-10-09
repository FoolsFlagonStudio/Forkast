package com.forkast.backend.pricing;

import java.math.BigDecimal;

import com.forkast.backend.ingest.Unit;

/** What a package holds: 3 LB, 16.9 FL_OZ, 12 COUNT. */
public record PackageSize(BigDecimal quantity, Unit unit) {
}