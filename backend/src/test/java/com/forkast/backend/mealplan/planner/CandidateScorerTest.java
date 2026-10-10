package com.forkast.backend.mealplan.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.forkast.backend.mealplan.MealSlot;

/** One test per factor, starting from a plain candidate that weighs 0.8. */
class CandidateScorerTest {

    private static final LocalDate SUNDAY = LocalDate.parse("2026-10-11");

    private final CandidateScorer scorer = new CandidateScorer(PlannerProperties.defaults());

    private static Candidate candidate(double fitPenalty, String cost, boolean complete, boolean favorite,
            LocalDate lastServed) {
        return new Candidate(UUID.randomUUID(), 4, 80, fitPenalty, 50, new BigDecimal(cost), complete, favorite,
                lastServed);
    }

    private static final Candidate PLAIN = candidate(0, "2.00", true, false, null);

    private static SlotRun run(int days) {
        List<LocalDate> dates = java.util.stream.IntStream.range(0, days).mapToObj(SUNDAY::plusDays).toList();
        return new SlotRun(MealSlot.DINNER, 1, dates, null, false);
    }

    private static CandidateScorer.Context context(boolean prefersMealPrep, String target) {
        return new CandidateScorer.Context(prefersMealPrep, target == null ? null : new BigDecimal(target), SUNDAY, 14);
    }

    private double weight(Candidate c, SlotRun run, CandidateScorer.Context ctx) {
        return scorer.weight(c, run, ctx);
    }

    @Test
    void slotScoreAlone() {
        assertThat(weight(PLAIN, run(1), context(false, null))).isCloseTo(0.8, within(1e-9));
    }

    @Test
    void nutritionPenaltyHalvesAtOne() {
        assertThat(weight(candidate(1.0, "2.00", true, false, null), run(1), context(false, null)))
                .isCloseTo(0.4, within(1e-9));
    }

    @Test
    void leftoversFavorRecipesThatKeep() {
        // 0.4 + 0.6 x 0.5 = 0.7, for a 3-day run or when the user prefers meal prep
        assertThat(weight(PLAIN, run(3), context(false, null))).isCloseTo(0.56, within(1e-9));
        assertThat(weight(PLAIN, run(1), context(true, null))).isCloseTo(0.56, within(1e-9));
    }

    @Test
    void overItsShareLosesWeightInProportionButCheaperGetsNoBoost() {
        assertThat(weight(PLAIN, run(1), context(false, "1.00"))).isCloseTo(0.4, within(1e-9));
        assertThat(weight(candidate(0, "0.50", true, false, null), run(1), context(false, "1.00")))
                .isCloseTo(0.8, within(1e-9));
    }

    @Test
    void favoritesComeUpMoreOften() {
        assertThat(weight(candidate(0, "2.00", true, true, null), run(1), context(false, null)))
                .isCloseTo(1.2, within(1e-9));
    }

    @Test
    void recentlyServedWithinTwiceTheWindow() {
        Candidate threeWeeksAgo = candidate(0, "2.00", true, false, SUNDAY.minusDays(21));
        Candidate sixWeeksAgo = candidate(0, "2.00", true, false, SUNDAY.minusDays(40));

        assertThat(weight(threeWeeksAgo, run(1), context(false, null))).isCloseTo(0.56, within(1e-9));
        assertThat(weight(sixWeeksAgo, run(1), context(false, null))).isCloseTo(0.8, within(1e-9));
    }

    @Test
    void incompleteCostCountsAgainst() {
        assertThat(weight(candidate(0, "2.00", false, false, null), run(1), context(false, null)))
                .isCloseTo(0.68, within(1e-9));
    }
}