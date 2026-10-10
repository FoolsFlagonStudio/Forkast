package com.forkast.backend.mealplan.dto;

import jakarta.validation.constraints.NotNull;

/** Lock a selection so regenerating keeps it, or unlock it. */
public record SelectionUpdateRequest(@NotNull Boolean locked) {
}