package com.forkast.backend.mealplan;

import static com.forkast.backend.common.ValidationUtils.requireNonNull;

import java.time.LocalDate;
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

/**
 * One meal on the calendar: a date and a slot, eaten from a selection. The
 * first day of a
 * selection is cooked; the rest are leftovers. The slot usually matches the
 * selection's, but a
 * dinner's leftovers can be the next day's lunch.
 *
 * Created through {@link PlanSelection#addMeal}, which sets both the selection
 * and the plan.
 */
@Entity
@Table(name = "planned_meals", uniqueConstraints = @UniqueConstraint(name = "uk_planned_meals", columnNames = {
        "meal_plan_id", "meal_date", "meal_slot" }))
public class PlannedMeal {

    // ---------- id ----------

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---------- columns ----------

    @Column(name = "meal_date", nullable = false, updatable = false)
    private LocalDate mealDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_slot", nullable = false, length = 10, updatable = false)
    private MealSlot mealSlot;

    @Column(nullable = false, updatable = false)
    private boolean leftover;

    // ---------- relationships ----------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_id", nullable = false, updatable = false)
    private MealPlan mealPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "selection_id", nullable = false, updatable = false)
    private PlanSelection selection;

    // ---------- constructors ----------

    protected PlannedMeal() {
        // required by JPA
    }

    PlannedMeal(PlanSelection selection, LocalDate mealDate, MealSlot mealSlot, boolean leftover) {
        this.selection = requireNonNull(selection, "selection");
        this.mealPlan = requireNonNull(selection.getMealPlan(), "selection's meal plan");
        this.mealDate = requireNonNull(mealDate, "mealDate");
        this.mealSlot = requireNonNull(mealSlot, "mealSlot");
        this.leftover = leftover;
    }

    // ---------- getters ----------

    public UUID getId() {
        return id;
    }

    public LocalDate getMealDate() {
        return mealDate;
    }

    public MealSlot getMealSlot() {
        return mealSlot;
    }

    public boolean isLeftover() {
        return leftover;
    }

    public PlanSelection getSelection() {
        return selection;
    }

    /** The recipe eaten at this meal, from its selection. */
    public Recipe getRecipe() {
        return selection.getRecipe();
    }

    public MealPlan getMealPlan() {
        return mealPlan;
    }
}