package com.forkast.backend.mealplan;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Date rules for plans, kept apart from the service so they're unit-tested. */
public final class PlanWeeks {

    /** One meal on the calendar: a date and a slot. */
    public record DaySlot(LocalDate date, MealSlot slot) {
    }

    private PlanWeeks() {
    }

    /**
     * The plan start day on or after today: today itself when today is the start
     * day.
     */
    public static LocalDate nextStart(LocalDate today, DayOfWeek startDay) {
        return today.with(TemporalAdjusters.nextOrSame(startDay));
    }

    /**
     * A week a user may plan: it begins on their start day and isn't over yet. A
     * week that has
     * already started is allowed (someone setting up on a Wednesday can still plan
     * this week).
     */
    public static void validateWeekStart(LocalDate weekStart, DayOfWeek startDay, LocalDate today) {
        if (weekStart.getDayOfWeek() != startDay) {
            throw new IllegalArgumentException("weekStart must be a " + name(startDay)
                    + ", your plan start day (" + weekStart + " is a " + name(weekStart.getDayOfWeek()) + ").");
        }
        if (weekStart.plusDays(MealPlan.DAYS - 1L).isBefore(today)) {
            throw new IllegalArgumentException("The week of " + weekStart + " is already over.");
        }
    }

    /**
     * The meals in the week that nothing covers, by date then slot: every date and
     * slot in
     * `slots` that isn't in `covered`.
     */
    public static List<DaySlot> openMeals(LocalDate weekStart, Collection<MealSlot> slots,
            Collection<DaySlot> covered) {
        Set<DaySlot> taken = new HashSet<>(covered);
        Set<MealSlot> ordered = slots.isEmpty() ? EnumSet.noneOf(MealSlot.class) : EnumSet.copyOf(slots);
        List<DaySlot> open = new ArrayList<>();
        for (int day = 0; day < MealPlan.DAYS; day++) {
            LocalDate date = weekStart.plusDays(day);
            for (MealSlot slot : ordered) {
                DaySlot meal = new DaySlot(date, slot);
                if (!taken.contains(meal)) {
                    open.add(meal);
                }
            }
        }
        return open;
    }

    private static String name(DayOfWeek day) {
        String lower = day.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}