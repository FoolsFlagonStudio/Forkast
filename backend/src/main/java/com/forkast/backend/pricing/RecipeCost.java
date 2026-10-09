package com.forkast.backend.pricing;

import java.math.BigDecimal;

/**
 * What one serving costs. complete is false when any required line couldn't be
 * priced; the
 * number then covers only the lines that could, like NutritionResult.
 * costPerServing is null
 * when no line could be priced at all.
 */
public record RecipeCost(BigDecimal costPerServing, boolean complete) {
}