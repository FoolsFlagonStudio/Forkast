package com.forkast.backend.mealplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PlanWeeksTest {

    private static final LocalDate WEDNESDAY = LocalDate.parse("2026-10-14");
    private static final LocalDate SUNDAY = LocalDate.parse("2026-10-11");

    @Test
    void nextStartIsTheComingStartDay() {
        assertThat(PlanWeeks.nextStart(WEDNESDAY, DayOfWeek.SUNDAY)).isEqualTo(LocalDate.parse("2026-10-18"));
        assertThat(PlanWeeks.nextStart(WEDNESDAY, DayOfWeek.MONDAY)).isEqualTo(LocalDate.parse("2026-10-19"));
    }

    @Test
    void nextStartIsTodayOnTheStartDay() {
        assertThat(PlanWeeks.nextStart(SUNDAY, DayOfWeek.SUNDAY)).isEqualTo(SUNDAY);
    }

    @Test
    void aWeekMustBeginOnTheStartDay() {
        assertThatThrownBy(() -> PlanWeeks.validateWeekStart(SUNDAY.plusDays(1), DayOfWeek.SUNDAY, WEDNESDAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a Sunday");
    }

    @Test
    void aWeekThatHasStartedCanBePlannedButNotOneThatsOver() {
        PlanWeeks.validateWeekStart(SUNDAY, DayOfWeek.SUNDAY, WEDNESDAY); // this week: fine
        PlanWeeks.validateWeekStart(SUNDAY, DayOfWeek.SUNDAY, SUNDAY.plusDays(6)); // its last day: fine

        assertThatThrownBy(() -> PlanWeeks.validateWeekStart(SUNDAY.minusDays(7), DayOfWeek.SUNDAY, WEDNESDAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already over");
    }

    @Test
    void openMealsAreEverySlotDayNothingCovers() {
        List<PlanWeeks.DaySlot> covered = List.of(
                new PlanWeeks.DaySlot(SUNDAY, MealSlot.DINNER),
                new PlanWeeks.DaySlot(SUNDAY.plusDays(1), MealSlot.DINNER),
                new PlanWeeks.DaySlot(SUNDAY.plusDays(1), MealSlot.LUNCH)); // a leftover lunch

        List<PlanWeeks.DaySlot> open = PlanWeeks.openMeals(SUNDAY, Set.of(MealSlot.DINNER, MealSlot.LUNCH), covered);

        assertThat(open).hasSize(14 - 3);
        assertThat(open.get(0)).isEqualTo(new PlanWeeks.DaySlot(SUNDAY, MealSlot.LUNCH)); // by date, then slot
        assertThat(open).doesNotContainAnyElementsOf(covered);
    }

    @Test
    void noSlotsMeansNothingOpen() {
        assertThat(PlanWeeks.openMeals(SUNDAY, Set.of(), List.of())).isEmpty();
    }
}