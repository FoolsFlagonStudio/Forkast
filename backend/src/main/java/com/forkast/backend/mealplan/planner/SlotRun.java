package com.forkast.backend.mealplan.planner;

import java.time.LocalDate;
import java.util.List;

import com.forkast.backend.mealplan.MealSlot;

/**
 * One planned selection before a recipe is chosen: the slot, its position in
 * that slot, the
 * dates it's eaten in that slot (the first is the cook day), and, with
 * leftovers for lunch, the
 * next day's lunch it also covers.
 *
 * stretched = longer than the freshness limit, allowed only because the user's
 * count can't
 * cover the week otherwise; only recipes that hold well may fill it.
 */
public record SlotRun(
        MealSlot slot,
        int position,
        List<LocalDate> dates,
        LocalDate leftoverLunch,
        boolean stretched) {

    public SlotRun {
        dates = List.copyOf(dates);
        if (dates.isEmpty()) {
            throw new IllegalArgumentException("a run needs at least one date");
        }
    }

    public LocalDate cookDate() {
        return dates.get(0);
    }

    /** Meals eaten from this one cook: its slot's days plus a leftover lunch. */
    public int meals() {
        return dates.size() + (leftoverLunch == null ? 0 : 1);
    }

    public int servings(int household) {
        return household * meals();
    }
}