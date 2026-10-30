package com.forkast.backend.mealplan.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.forkast.backend.mealplan.MealPlan;
import com.forkast.backend.mealplan.PlanSource;

/** One row in the plan list. */
public record MealPlanSummaryResponse(
        UUID id,
        LocalDate weekStart,
        LocalDate weekEnd,
        PlanSource source,
        BigDecimal budget,
        BigDecimal usageCost,
        BigDecimal groceryCost,
        boolean overBudget,
        int recipes,
        int openMeals,
        Instant generatedAt) {

    public static MealPlanSummaryResponse from(MealPlan plan) {
        return new MealPlanSummaryResponse(plan.getId(), plan.getWeekStartDate(), plan.getWeekEndDate(),
                plan.getSource(), plan.getBudgetTarget(), plan.getUsageCost(), plan.getGroceryCost(),
                plan.isOverBudget(), plan.getSelections().size(), plan.getOpenMeals(), plan.getGeneratedAt());
    }
}