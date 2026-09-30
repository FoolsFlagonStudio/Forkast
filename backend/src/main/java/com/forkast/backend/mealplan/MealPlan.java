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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "meal_plans",
       uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "week_start_date" }))
public class MealPlan {

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
    private List<PlannedMeal> plannedMeals = new ArrayList<>();

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroceryListItem> groceryItems = new ArrayList<>();

    // ---------- constructors ----------

    protected MealPlan() {
        // required by JPA
    }

    public MealPlan(User user, LocalDate weekStartDate, BigDecimal budgetTarget) {
        this.user = requireNonNull(user, "user");
        this.weekStartDate = requireNonNull(weekStartDate, "weekStartDate");
        setBudgetTarget(budgetTarget);
    }

    // ---------- relationship helpers ----------

    public void addPlannedMeal(PlannedMeal meal) {
        plannedMeals.add(meal);
        meal.setMealPlan(this);
    }

    public void removePlannedMeal(PlannedMeal meal) {
        plannedMeals.remove(meal);
        meal.setMealPlan(null);
    }

    public void addGroceryItem(GroceryListItem item) {
        groceryItems.add(item);
        item.setMealPlan(this);
    }

    public void removeGroceryItem(GroceryListItem item) {
        groceryItems.remove(item);
        item.setMealPlan(null);
    }

    /** Wipe the grocery list so it can be regenerated after meals change. */
    public void clearGroceryList() {
        for (GroceryListItem item : groceryItems) {
            item.setMealPlan(null);
        }
        groceryItems.clear();
    }

    // ---------- derived values ----------

    /** Sum of estimated costs; items without a price are skipped. */
    public BigDecimal getEstimatedTotalCost() {
        return groceryItems.stream()
                .map(GroceryListItem::getEstimatedCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isOverBudget() {
        return budgetTarget != null
                && getEstimatedTotalCost().compareTo(budgetTarget) > 0;
    }

    // ---------- getters / setters ----------

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public LocalDate getWeekStartDate() { return weekStartDate; }

    public BigDecimal getBudgetTarget() { return budgetTarget; }
    public void setBudgetTarget(BigDecimal budgetTarget) {
        if (budgetTarget != null && budgetTarget.signum() <= 0) {
            throw new IllegalArgumentException("budgetTarget must be greater than 0");
        }
        this.budgetTarget = budgetTarget;
    }

    public boolean isMealPrepMode() { return mealPrepMode; }
    public void setMealPrepMode(boolean mealPrepMode) { this.mealPrepMode = mealPrepMode; }

    /** Meals sorted Monday to Sunday, breakfast to dinner. */
    public List<PlannedMeal> getPlannedMeals() {
        List<PlannedMeal> sorted = new ArrayList<>(plannedMeals);
        sorted.sort(Comparator.comparing(PlannedMeal::getDayOfWeek)
                              .thenComparing(PlannedMeal::getMealSlot));
        return Collections.unmodifiableList(sorted);
    }

    public List<GroceryListItem> getGroceryItems() {
        return Collections.unmodifiableList(groceryItems);
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}