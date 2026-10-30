package com.forkast.backend.mealplan;

import static com.forkast.backend.common.ValidationUtils.requireNonNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.forkast.backend.user.User;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One user's week: recipe selections (each with its calendar rows) and a
 * grocery list.
 * weekStartDate is the date of the user's plan start day; the plan runs 7 days
 * from it.
 *
 * Costs are stored, not summed on read: usageCost is what the meals use,
 * groceryCost what the
 * shopping trip costs. overBudget compares groceryCost (usageCost until a list
 * exists) with the
 * budget snapshot taken at generation.
 */
@Entity
@Table(name = "meal_plans", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "week_start_date" }))
public class MealPlan {

    public static final int DAYS = 7;

    // ---------- id ----------

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---------- columns ----------

    @Column(name = "week_start_date", nullable = false, updatable = false)
    private LocalDate weekStartDate;

    @Column(name = "budget_target", precision = 10, scale = 2)
    private BigDecimal budgetTarget;

    @Column(name = "is_meal_prep_mode", nullable = false)
    private boolean mealPrepMode = false;

    @Column(name = "generation_seed")
    private Long generationSeed;

    @Column(name = "usage_cost", precision = 10, scale = 2)
    private BigDecimal usageCost;

    @Column(name = "grocery_cost", precision = 10, scale = 2)
    private BigDecimal groceryCost;

    @Column(name = "over_budget", nullable = false)
    private boolean overBudget = false;

    @Column(name = "open_meals", nullable = false)
    private int openMeals = 0;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private PlanSource source = PlanSource.MANUAL;

    /**
     * When the weekly job wrote recipe_history for this plan; null until the week
     * has ended
     */
    @Column(name = "history_recorded_at")
    private Instant historyRecordedAt;

    // ---------- timestamps ----------

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ---------- relationships ----------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanSelection> selections = new ArrayList<>();

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroceryListItem> groceryItems = new ArrayList<>();

    // ---------- constructors ----------

    protected MealPlan() {
        // required by JPA
    }

    public MealPlan(User user, LocalDate weekStartDate, BigDecimal budgetTarget, PlanSource source) {
        this.user = requireNonNull(user, "user");
        this.weekStartDate = requireNonNull(weekStartDate, "weekStartDate");
        this.source = requireNonNull(source, "source");
        setBudgetTarget(budgetTarget);
    }

    // ---------- relationship helpers ----------

    public void addSelection(PlanSelection selection) {
        selections.add(selection);
        selection.setMealPlan(this);
    }

    public void removeSelection(PlanSelection selection) {
        selections.remove(selection);
        selection.setMealPlan(null);
    }

    public void addGroceryItem(GroceryListItem item) {
        groceryItems.add(item);
        item.setMealPlan(this);
    }

    public void removeGroceryItem(GroceryListItem item) {
        groceryItems.remove(item);
        item.setMealPlan(null);
    }

    /** Wipe the grocery list so it can be rebuilt after meals change. */
    public void clearGroceryList() {
        for (GroceryListItem item : groceryItems) {
            item.setMealPlan(null);
        }
        groceryItems.clear();
    }

    // ---------- generation and costs ----------

    /**
     * Records how the current selections were drawn, so the same seed rebuilds
     * them.
     */
    public void recordGeneration(long seed, Instant at) {
        this.generationSeed = seed;
        this.generatedAt = requireNonNull(at, "generatedAt");
    }

    /**
     * Stores both costs and recomputes overBudget. groceryCost may be null before
     * the list is
     * built; the check then uses usageCost. openMeals = meals in the week no recipe
     * covers.
     */
    public void setCosts(BigDecimal usageCost, BigDecimal groceryCost, int openMeals) {
        if ((usageCost != null && usageCost.signum() < 0) || (groceryCost != null && groceryCost.signum() < 0)) {
            throw new IllegalArgumentException("costs cannot be negative");
        }
        if (openMeals < 0) {
            throw new IllegalArgumentException("openMeals cannot be negative");
        }
        this.usageCost = usageCost;
        this.groceryCost = groceryCost;
        this.openMeals = openMeals;
        BigDecimal spend = groceryCost != null ? groceryCost : usageCost;
        this.overBudget = budgetTarget != null && spend != null && spend.compareTo(budgetTarget) > 0;
    }

    /**
     * Marks the plan's meals as written to recipe_history, so the job doesn't write
     * them twice.
     */
    public void markHistoryRecorded(Instant at) {
        this.historyRecordedAt = requireNonNull(at, "historyRecordedAt");
    }

    /** The plan's last day (start + 6). */
    public LocalDate getWeekEndDate() {
        return weekStartDate.plusDays(DAYS - 1L);
    }

    // ---------- getters / setters ----------

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public BigDecimal getBudgetTarget() {
        return budgetTarget;
    }

    public void setBudgetTarget(BigDecimal budgetTarget) {
        if (budgetTarget != null && budgetTarget.signum() <= 0) {
            throw new IllegalArgumentException("budgetTarget must be greater than 0");
        }
        this.budgetTarget = budgetTarget;
    }

    public boolean isMealPrepMode() {
        return mealPrepMode;
    }

    public void setMealPrepMode(boolean mealPrepMode) {
        this.mealPrepMode = mealPrepMode;
    }

    public Long getGenerationSeed() {
        return generationSeed;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public PlanSource getSource() {
        return source;
    }

    public Instant getHistoryRecordedAt() {
        return historyRecordedAt;
    }

    public BigDecimal getUsageCost() {
        return usageCost;
    }

    public BigDecimal getGroceryCost() {
        return groceryCost;
    }

    public boolean isOverBudget() {
        return overBudget;
    }

    public int getOpenMeals() {
        return openMeals;
    }

    /** Selections by slot (breakfast to dinner), then position. */
    public List<PlanSelection> getSelections() {
        List<PlanSelection> sorted = new ArrayList<>(selections);
        sorted.sort(Comparator.comparing(PlanSelection::getMealSlot).thenComparing(PlanSelection::getPosition));
        return Collections.unmodifiableList(sorted);
    }

    /** The calendar: every selection's meals, by date, breakfast to dinner. */
    public List<PlannedMeal> getPlannedMeals() {
        return selections.stream()
                .flatMap(s -> s.getMeals().stream())
                .sorted(Comparator.comparing(PlannedMeal::getMealDate).thenComparing(PlannedMeal::getMealSlot))
                .toList();
    }

    public List<GroceryListItem> getGroceryItems() {
        return Collections.unmodifiableList(groceryItems);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}