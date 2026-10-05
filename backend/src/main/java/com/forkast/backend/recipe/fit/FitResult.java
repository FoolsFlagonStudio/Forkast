package com.forkast.backend.recipe.fit;

import java.util.List;

/**
 * The flags for one recipe plus a penalty for sorting: 0 is a perfect fit, and
 * each miss
 * adds how far off it is as a fraction of the target (20% over the calorie
 * limit adds 0.2).
 */
public record FitResult(List<FitFlag> flags, double penalty) {

    public boolean fits() {
        return flags.isEmpty();
    }
}