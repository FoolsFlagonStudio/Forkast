package com.forkast.backend.user;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.forkast.backend.diet.DietaryLabel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToMany
    @JoinTable(name = "user_dietary_restrictions", joinColumns = @JoinColumn(name = "user_preference_id"), inverseJoinColumns = @JoinColumn(name = "dietary_label_id"))
    private Set<DietaryLabel> dietaryRestrictions = new HashSet<>();

    

    @Column(name = "weekly_budget", nullable = false, precision = 10, scale = 2)
    private BigDecimal weeklyBudget;

    @Column(name = "household_serving_size", nullable = false)
    private int householdServingSize = 1;

    @Column(name = "max_calories_per_meal")
    private Integer maxCaloriesPerMeal;

    @Column(name = "max_calories_per_day")
    private Integer maxCaloriesPerDay;

    @Column(name = "protein_target_grams")
    private Integer proteinTargetGrams;

    @Column(name = "carbs_target_grams")
    private Integer carbsTargetGrams;

    @Column(name = "fat_target_grams")
    private Integer fatTargetGrams;

    @Column(name = "repeat_avoidance_days", nullable = false)
    private int repeatAvoidanceDays = 14;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserPreferences() {
    }

    public UserPreferences(User user, BigDecimal weeklyBudget) {
        this.user = user;
        this.weeklyBudget = weeklyBudget;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getWeeklyBudget() {
        return weeklyBudget;
    }

    public void setWeeklyBudget(BigDecimal weeklyBudget) {
        this.weeklyBudget = weeklyBudget;
    }

    public int getHouseholdServingSize() {
        return householdServingSize;
    }

    public void setHouseholdServingSize(int householdServingSize) {
        this.householdServingSize = householdServingSize;
    }

    public Integer getMaxCaloriesPerMeal() {
        return maxCaloriesPerMeal;
    }

    public void setMaxCaloriesPerMeal(Integer maxCaloriesPerMeal) {
        this.maxCaloriesPerMeal = maxCaloriesPerMeal;
    }

    public Integer getMaxCaloriesPerDay() {
        return maxCaloriesPerDay;
    }

    public void setMaxCaloriesPerDay(Integer maxCaloriesPerDay) {
        this.maxCaloriesPerDay = maxCaloriesPerDay;
    }

    public Integer getProteinTargetGrams() {
        return proteinTargetGrams;
    }

    public void setProteinTargetGrams(Integer proteinTargetGrams) {
        this.proteinTargetGrams = proteinTargetGrams;
    }

    public Integer getCarbsTargetGrams() {
        return carbsTargetGrams;
    }

    public void setCarbsTargetGrams(Integer carbsTargetGrams) {
        this.carbsTargetGrams = carbsTargetGrams;
    }

    public Integer getFatTargetGrams() {
        return fatTargetGrams;
    }

    public void setFatTargetGrams(Integer fatTargetGrams) {
        this.fatTargetGrams = fatTargetGrams;
    }

    public int getRepeatAvoidanceDays() {
        return repeatAvoidanceDays;
    }

    public void setRepeatAvoidanceDays(int repeatAvoidanceDays) {
        this.repeatAvoidanceDays = repeatAvoidanceDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<DietaryLabel> getDietaryRestrictions() {
        return Collections.unmodifiableSet(dietaryRestrictions);
    }

    public void addDietaryRestriction(DietaryLabel label) {
        dietaryRestrictions.add(label);
    }

    public void removeDietaryRestriction(DietaryLabel label) {
        dietaryRestrictions.remove(label);
    }
}
