package com.forkast.backend.ingest;

import com.forkast.backend.mealplan.MealSlot;

/**
 * How well a recipe fits each meal slot, 0 to 100. A recipe can fill a slot at
 * 50 or more.
 */
public record SlotScores(int breakfast, int lunch, int dinner) {

    public static final int FITS = 50;

    public int of(MealSlot slot) {
        return switch (slot) {
            case BREAKFAST -> breakfast;
            case LUNCH -> lunch;
            case DINNER -> dinner;
        };
    }

    public boolean fits(MealSlot slot) {
        return of(slot) >= FITS;
    }
}