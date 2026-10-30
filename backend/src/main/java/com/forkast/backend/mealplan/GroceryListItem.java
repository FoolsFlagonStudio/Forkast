package com.forkast.backend.mealplan;

import static com.forkast.backend.common.ValidationUtils.requireNonNull;
import static com.forkast.backend.common.ValidationUtils.requirePositive;
import static com.forkast.backend.common.ValidationUtils.requireText;

import java.math.BigDecimal;
import java.util.UUID;

import com.forkast.backend.ingredient.Ingredient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "grocery_list_items", uniqueConstraints = @UniqueConstraint(columnNames = { "meal_plan_id",
        "ingredient_id", "unit" }))
public class GroceryListItem {

    // ---------- id ----------

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---------- columns ----------

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 3)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(nullable = false)
    private boolean purchased = false;

    /** Whole packages to buy; null for by-weight items and lines with no product */
    @Column(name = "package_count")
    private Integer packageCount;

    @Column(name = "package_label")
    private String packageLabel;

    @Column(name = "package_price", precision = 10, scale = 2)
    private BigDecimal packagePrice;

    /** A pantry staple: listed under "check your pantry", counted at usage cost */
    @Column(nullable = false)
    private boolean pantry = false;

    /** The user needs to buy this staple, so it counts at its full package price */
    @Column(name = "need_to_buy", nullable = false)
    private boolean needToBuy = false;

    // ---------- relationships ----------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false, updatable = false)
    private Ingredient ingredient;

    // ---------- constructors ----------

    protected GroceryListItem() {
        // required by JPA
    }

    public GroceryListItem(Ingredient ingredient, BigDecimal totalAmount, String unit) {
        this.ingredient = requireNonNull(ingredient, "ingredient");
        this.totalAmount = requirePositive(totalAmount, "totalAmount");
        this.unit = requireText(unit, "unit");
    }

    // ---------- behavior ----------

    /** Replaces the amount when the list is rebuilt after the meals change. */
    public void updateAmount(BigDecimal totalAmount) {
        this.totalAmount = requirePositive(totalAmount, "totalAmount");
    }

    /** Combine another recipe's usage of the same ingredient into this line. */
    public void addAmount(BigDecimal amount) {
        this.totalAmount = this.totalAmount.add(requirePositive(amount, "amount"));
    }

    public void markPurchased() {
        this.purchased = true;
    }

    public void markUnpurchased() {
        this.purchased = false;
    }

    /**
     * How the line is bought: count x label at price each. All null when there's no
     * package.
     */
    public void setPackage(Integer count, String label, BigDecimal price) {
        if (count != null && count < 1) {
            throw new IllegalArgumentException("packageCount must be at least 1");
        }
        if (price != null && price.signum() < 0) {
            throw new IllegalArgumentException("packagePrice cannot be negative");
        }
        this.packageCount = count;
        this.packageLabel = label == null || label.isBlank() ? null : label.trim();
        this.packagePrice = price;
    }

    public void setPantry(boolean pantry) {
        this.pantry = pantry;
    }

    public void setNeedToBuy(boolean needToBuy) {
        this.needToBuy = needToBuy;
    }

    // ---------- getters / setters ----------

    public UUID getId() {
        return id;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public Integer getPackageCount() {
        return packageCount;
    }

    public String getPackageLabel() {
        return packageLabel;
    }

    public BigDecimal getPackagePrice() {
        return packagePrice;
    }

    public boolean isPantry() {
        return pantry;
    }

    public boolean isNeedToBuy() {
        return needToBuy;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        if (estimatedCost != null && estimatedCost.signum() < 0) {
            throw new IllegalArgumentException("estimatedCost cannot be negative");
        }
        this.estimatedCost = estimatedCost;
    }

    public MealPlan getMealPlan() {
        return mealPlan;
    }

    void setMealPlan(MealPlan mealPlan) {
        this.mealPlan = mealPlan;
    } // package-private
}