package com.forkast.backend.ingredient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredients")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "fdc_id", unique = true)
    private Long fdcId;

    @Column(nullable = false)
    private String name;

    @Column(name = "calories_per_100g", precision = 7, scale = 2)
    private BigDecimal caloriesPer100g;

    @Column(name = "protein_g_per_100g", precision = 7, scale = 2)
    private BigDecimal proteinGPer100g;

    @Column(name = "carbs_g_per_100g", precision = 7, scale = 2)
    private BigDecimal carbsGPer100g;

    @Column(name = "fat_g_per_100g", precision = 7, scale = 2)
    private BigDecimal fatGPer100g;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("gramWeight ASC")
    private List<IngredientPortion> portions = new ArrayList<>();

    protected Ingredient() {
    }

    public Ingredient(String name) {
        setName(name);
    }

    // --- relationship helpers ---

    public void addPortion(IngredientPortion portion) {
        portions.add(portion);
        portion.setIngredient(this);
    }

    public void removePortion(IngredientPortion portion) {
        portions.remove(portion);
        portion.setIngredient(null);
    }

    // --- getters / setters ---

    public UUID getId() {
        return id;
    }

    public Long getFdcId() {
        return fdcId;
    }

    public void setFdcId(Long fdcId) {
        this.fdcId = fdcId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public BigDecimal getCaloriesPer100g() {
        return caloriesPer100g;
    }

    public void setCaloriesPer100g(BigDecimal v) {
        this.caloriesPer100g = v;
    }

    public BigDecimal getProteinGPer100g() {
        return proteinGPer100g;
    }

    public void setProteinGPer100g(BigDecimal v) {
        this.proteinGPer100g = v;
    }

    public BigDecimal getCarbsGPer100g() {
        return carbsGPer100g;
    }

    public void setCarbsGPer100g(BigDecimal v) {
        this.carbsGPer100g = v;
    }

    public BigDecimal getFatGPer100g() {
        return fatGPer100g;
    }

    public void setFatGPer100g(BigDecimal v) {
        this.fatGPer100g = v;
    }

    public List<IngredientPortion> getPortions() {
        return Collections.unmodifiableList(portions);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}