package com.forkast.backend.mealplan.planner;

import java.util.List;

/**
 * The runs to fill, and how many slot-days nothing covers (a count of 1, say).
 */
public record WeekShape(List<SlotRun> runs, int openMeals) {

    public WeekShape {
        runs = List.copyOf(runs);
    }
}