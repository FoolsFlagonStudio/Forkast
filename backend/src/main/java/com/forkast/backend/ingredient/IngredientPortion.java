package com.forkast.backend.ingredient;
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
@Table(name = "ingredient_portions",
       uniqueConstraints = @UniqueConstraint(columnNames = { "ingredient_id", "description" }))
public class IngredientPortion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String description;

    @Column(name = "gram_weight", nullable = false, precision = 8, scale = 2)
    private BigDecimal gramWeight;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    protected IngredientPortion() {}

    public IngredientPortion(String description, BigDecimal gramWeight) {
        setDescription(description);
        setGramWeight(gramWeight);
    }

    public UUID getId() { return id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description.trim(); }

    public BigDecimal getGramWeight() { return gramWeight; }
    public void setGramWeight(BigDecimal gramWeight) {
        if (gramWeight == null || gramWeight.signum() <= 0) {
            throw new IllegalArgumentException("gramWeight must be greater than 0");
        }
        this.gramWeight = gramWeight;
    }

    public Ingredient getIngredient() { return ingredient; }

    void setIngredient(Ingredient ingredient) {   // package-private: only Ingredient calls this
        this.ingredient = ingredient;
    }
}