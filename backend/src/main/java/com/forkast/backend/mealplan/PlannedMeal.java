package com.forkast.backend.mealplan;

import static com.forkast.backend.common.ValidationUtils.requireNonNull;
import static com.forkast.backend.common.ValidationUtils.requirePositive;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.UUID;

import com.forkast.backend.recipe.Recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "planned_meals", uniqueConstraints = @UniqueConstraint(columnNames = { "meal_plan_id", "day_of_week",
        "meal_slot" }))
public class PlannedMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_slot", nullable = false, length = 10)
    private MealSlot mealSlot;

    @Column(name = "portion_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal portionMultiplier = BigDecimal.ONE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    protected PlannedMeal() {
        // required by JPA
    }

    public PlannedMeal(Recipe recipe, DayOfWeek dayOfWeek, MealSlot mealSlot) {
        setRecipe(recipe);
        moveTo(dayOfWeek, mealSlot);
    }

    /** Move this meal to a different day and/or slot. */
    public void moveTo(DayOfWeek dayOfWeek, MealSlot mealSlot) {
        this.dayOfWeek = requireNonNull(dayOfWeek, "dayOfWeek");
        this.mealSlot = requireNonNull(mealSlot, "mealSlot");
    }

    public UUID getId() {
        return id;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public MealSlot getMealSlot() {
        return mealSlot;
    }

    public BigDecimal getPortionMultiplier() {
        return portionMultiplier;
    }

    public void setPortionMultiplier(BigDecimal portionMultiplier) {
        this.portionMultiplier = requirePositive(portionMultiplier, "portionMultiplier");
    }

    public Recipe getRecipe() {
        return recipe;
    }

    /** Swap in a different recipe for this slot. */
    public void setRecipe(Recipe recipe) {
        this.recipe = requireNonNull(recipe, "recipe");
    }

    public MealPlan getMealPlan() {
        return mealPlan;
    }

    void setMealPlan(MealPlan mealPlan) {
        this.mealPlan = mealPlan;
    } // package-private
}