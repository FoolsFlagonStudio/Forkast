package com.forkast.backend.recipe;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.forkast.backend.diet.DietaryLabel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipes")
public class Recipe {

    // ---------- id ----------

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---------- columns ----------

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "prep_time_minutes")
    private Integer prepTimeMinutes;

    @Column(name = "cook_time_minutes")
    private Integer cookTimeMinutes;

    @Column(name = "base_servings", nullable = false, updatable = false)
    private int baseServings;

    @Column(columnDefinition = "text")
    private String notes;

    // ---------- timestamps ----------

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ---------- relationships ----------

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC")
    private List<RecipeStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "recipe_dietary_labels", joinColumns = @JoinColumn(name = "recipe_id"), inverseJoinColumns = @JoinColumn(name = "dietary_label_id"))
    private Set<DietaryLabel> dietaryLabels = new HashSet<>();

    // ---------- constructors ----------

    protected Recipe() {
        // required by JPA
    }

    public Recipe(String name, int baseServings) {
        setName(name);
        if (baseServings < 1) {
            throw new IllegalArgumentException("baseServings must be at least 1");
        }
        this.baseServings = baseServings;
    }

    // ---------- relationship helpers ----------

    public void addStep(RecipeStep step) {
        steps.add(step);
        step.setRecipe(this);
    }

    public void removeStep(RecipeStep step) {
        steps.remove(step);
        step.setRecipe(null);
    }

    public void addIngredient(RecipeIngredient ingredient) {
        ingredients.add(ingredient);
        ingredient.setRecipe(this);
    }

    public void removeIngredient(RecipeIngredient ingredient) {
        ingredients.remove(ingredient);
        ingredient.setRecipe(null);
    }

    public void addDietaryLabel(DietaryLabel label) {
        dietaryLabels.add(label);
    }

    public void removeDietaryLabel(DietaryLabel label) {
        dietaryLabels.remove(label);
    }

    // ---------- getters / setters ----------

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "name");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = trimToNull(description);
    }

    public Integer getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public void setPrepTimeMinutes(Integer prepTimeMinutes) {
        this.prepTimeMinutes = requireNonNegative(prepTimeMinutes, "prepTimeMinutes");
    }

    public Integer getCookTimeMinutes() {
        return cookTimeMinutes;
    }

    public void setCookTimeMinutes(Integer cookTimeMinutes) {
        this.cookTimeMinutes = requireNonNegative(cookTimeMinutes, "cookTimeMinutes");
    }

    /** Derived from prep + cook time; not stored. */
    public Integer getTotalTimeMinutes() {
        if (prepTimeMinutes == null && cookTimeMinutes == null) {
            return null;
        }
        int prep = prepTimeMinutes == null ? 0 : prepTimeMinutes;
        int cook = cookTimeMinutes == null ? 0 : cookTimeMinutes;
        return prep + cook;
    }

    public int getBaseServings() {
        return baseServings;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = trimToNull(notes);
    }

    public List<RecipeStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public List<RecipeIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public Set<DietaryLabel> getDietaryLabels() {
        return Collections.unmodifiableSet(dietaryLabels);
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

    private static Integer requireNonNegative(Integer value, String field) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
        return value;
    }
}