package com.forkast.backend.history;

import java.time.Instant;
import java.util.UUID;

/** The most recent recipe_history row for one recipe. */
public record LastServedAt(UUID recipeId, Instant servedAt) {
}