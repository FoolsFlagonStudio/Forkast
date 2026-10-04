package com.forkast.backend.ingredient;
import static com.forkast.backend.common.ValidationUtils.requireText;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredients")
public class Ingredient {

    // ---------- id ----------

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---------- columns ----------

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

    // ---------- timestamps ----------

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ---------- relationships ----------

    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("gramWeight ASC")
    private List<IngredientPortion> portions = new ArrayList<>();

    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IngredientAlias> aliases = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "ingredient_tags", joinColumns = @JoinColumn(name = "ingredient_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 20)
    private Set<IngredientTag> tags = new HashSet<>();

    // ---------- constructors ----------

    protected Ingredient() {
    }

    public Ingredient(String name) {
        setName(name);
    }

    // ---------- relationship helpers ----------

    public void addPortion(IngredientPortion portion) {
        portions.add(portion);
        portion.setIngredient(this);
    }

    public void removePortion(IngredientPortion portion) {
        portions.remove(portion);
        portion.setIngredient(null);
    }

    // ---------- getters / setters ----------

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
        this.name = requireText(name, "name").toLowerCase();
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

    /** Adds an alias unless it already exists or is the ingredient's own name. */
    public void addAlias(String alias) {
        String normalized = requireText(alias, "alias").toLowerCase();
        boolean exists = normalized.equals(name)
                || aliases.stream().anyMatch(a -> a.getAlias().equals(normalized));
        if (!exists) {
            IngredientAlias newAlias = new IngredientAlias(normalized);
            newAlias.setIngredient(this);
            aliases.add(newAlias);
        }
    }

    public Set<IngredientAlias> getAliases() {
        return Collections.unmodifiableSet(aliases);
    }

    public void replaceTags(Set<IngredientTag> newTags) {
        tags.clear();
        if (newTags != null) {
            tags.addAll(newTags);
        }
    }

    public Set<IngredientTag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /** Replaces all portions; used when re-importing from FDC. */
    public void replacePortions(List<IngredientPortion> newPortions) {
        for (IngredientPortion portion : List.copyOf(portions)) {
            removePortion(portion);
        }
        newPortions.forEach(this::addPortion);
    }
}