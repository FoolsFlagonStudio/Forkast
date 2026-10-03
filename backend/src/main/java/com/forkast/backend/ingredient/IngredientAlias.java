package com.forkast.backend.ingredient;

import static com.forkast.backend.common.ValidationUtils.requireText;

import java.util.UUID;

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
@Table(name = "ingredient_aliases")
public class IngredientAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private String alias;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    protected IngredientAlias() {
        // required by JPA
    }

    public IngredientAlias(String alias) {
        this.alias = requireText(alias, "alias").toLowerCase();
    }

    public UUID getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    void setIngredient(Ingredient ingredient) { // package-private: only Ingredient calls this
        this.ingredient = ingredient;
    }
}