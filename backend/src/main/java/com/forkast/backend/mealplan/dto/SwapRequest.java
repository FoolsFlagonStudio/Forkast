package com.forkast.backend.mealplan.dto;

import java.util.UUID;

/**
 * recipeId: the recipe to use. Left out (or no body at all): pick one for me.
 */
public record SwapRequest(UUID recipeId) {
}