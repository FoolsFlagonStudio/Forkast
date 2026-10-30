package com.forkast.backend.mealplan.planner;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The weight of a candidate for one run: the product of the plan doc's factors.
 * A higher weight
 * makes the recipe more likely in the weighted draw; it never guarantees or
 * rules it out.
 *
 * slot fit slot score / 100
 * nutrition 1 / (1 + fit penalty)
 * leftovers base + (1 - base) x meal-prep / 100, for runs of 2+ meals or
 * prefersMealPrep
 * cost min(1, target per serving / cost per serving): over its share loses
 * weight,
 * cheaper gets no boost (so plans aren't always the cheapest recipes)
 * favorite x favoriteBoost
 * recent x recentFactor if served within twice the repeat window
 * incomplete x incompleteCostFactor if the stored cost misses some lines
 */
public class CandidateScorer {

    /**
     * @param targetCostPerServing the budget's share per serving (budget x margin /
     *                             servings in
     *                             the week), or null for no budget
     */
    public record Context(boolean prefersMealPrep, BigDecimal targetCostPerServing, LocalDate weekStart,
            int repeatAvoidanceDays) {
    }

    private final PlannerProperties properties;

    public CandidateScorer(PlannerProperties properties) {
        this.properties = properties;
    }

    public double weight(Candidate c, SlotRun run, Context context) {
        double weight = c.slotScore() / 100.0;
        weight *= 1.0 / (1.0 + Math.max(0, c.fitPenalty()));

        if (run.meals() >= 2 || context.prefersMealPrep()) {
            double base = properties.leftoverBase();
            weight *= base + (1 - base) * c.mealPrepScore() / 100.0;
        }

        BigDecimal target = context.targetCostPerServing();
        BigDecimal cost = c.costPerServing();
        if (target != null && cost != null && cost.signum() > 0) {
            weight *= Math.min(1.0, target.doubleValue() / cost.doubleValue());
        }

        if (c.favorite()) {
            weight *= properties.favoriteBoost();
        }
        if (c.lastServed() != null && context.repeatAvoidanceDays() > 0
                && c.lastServed().isAfter(context.weekStart().minusDays(2L * context.repeatAvoidanceDays()))) {
            weight *= properties.recentFactor();
        }
        if (!c.costComplete()) {
            weight *= properties.incompleteCostFactor();
        }
        return weight;
    }
}