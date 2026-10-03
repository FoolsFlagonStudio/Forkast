package com.forkast.backend.diet;

import java.util.UUID;

public record DietaryLabelResponse(UUID id, String name) {
    public static DietaryLabelResponse from(DietaryLabel dietaryLabel) {
        return new DietaryLabelResponse(dietaryLabel.getId(), dietaryLabel.getName());
    }
}
