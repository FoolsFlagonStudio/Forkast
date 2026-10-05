package com.forkast.backend.recipe.scale;

import java.math.BigDecimal;

/**
 * amount is exact (3 decimals) for the grocery list to add up; displayAmount is
 * what a cook
 * reads ("1 1/3"). unit is the stored unit code, which can differ from the
 * recipe's when a
 * small amount is shown in a smaller measure (1/12 cup becomes 1 1/3 tbsp).
 * amount and
 * displayAmount are null for lines with no amount, like "salt to taste".
 */
public record ScaledAmount(BigDecimal amount, String unit, String displayAmount) {
}