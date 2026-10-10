package com.forkast.backend.mealplan;

import java.time.LocalDate;
import java.util.UUID;

/** When a user last had a recipe, as a date. */
public record LastServed(UUID recipeId, LocalDate date) {
}