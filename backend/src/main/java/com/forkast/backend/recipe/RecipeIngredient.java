package com.forkast.backend.recipe;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

@Entity
@Table(name = "recipe_ingredients")
public class RecipeIngredient {

    // ---------- id ----------

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---------- columns ----------

    @Column(precision = 10, scale = 3)
    private BigDecimal amount; // nullable: "salt to taste"

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "is_optional", nullable = false)
    private boolean optional = false;

    @Column(name = "prep_note")
    private String prepNote;

    @Column(name = "raw_text", nullable = false, columnDefinition = "text")
    private String rawText;

    @Column(name = "needs_review", nullable = false)
    private boolean needsReview = false;

    // ---------- timestamps ----------

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ---------- relationships ----------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY) // optional: unmatched ingredients
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    // ---------- constructors ----------

    protected RecipeIngredient() {
    }

    public RecipeIngredient(String rawText, BigDecimal amount, String unit) {
        setRawText(rawText);
        setAmount(amount);
        setUnit(unit);
        this.needsReview = true; // nothing is linked yet
    }

    // ---------- ingredient matching ----------

    public void matchIngredient(Ingredient ingredient) {
        if (ingredient == null) {
            throw new IllegalArgumentException("ingredient is required");
        }
        this.ingredient = ingredient;
        this.needsReview = false;
    }

    public void unmatchIngredient() {
        this.ingredient = null;
        this.needsReview = true;
    }

    // ---------- getters / setters ----------

    public UUID getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        if (amount != null && amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        this.amount = amount;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = requireText(unit, "unit");
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getPrepNote() {
        return prepNote;
    }

    public void setPrepNote(String prepNote) {
        this.prepNote = trimToNull(prepNote);
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = requireText(rawText, "rawText");
    }

    public boolean isNeedsReview() {
        return needsReview;
    }

    public void markNeedsReview() {
        this.needsReview = true;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    } // package-private

    public Ingredient getIngredient() {
        return ingredient;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // ---------- private helpers ----------

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}