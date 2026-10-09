package com.forkast.backend.pricing;

import java.util.List;

public record ProductMappingResult(int created, int updated, int unchanged, List<PriceImportResult.Failure> failed) {
}