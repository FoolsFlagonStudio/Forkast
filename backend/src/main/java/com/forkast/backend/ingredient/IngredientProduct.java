package com.forkast.backend.ingredient;

import static com.forkast.backend.common.ValidationUtils.requireNonNull;
import static com.forkast.backend.common.ValidationUtils.requireText;
import static com.forkast.backend.common.ValidationUtils.trimToNull;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A product at a price source that stands in for an ingredient: "yellow onion"
 * is Kroger
 * product 0000000004093 (loose yellow onions). Chosen once, like an FDC id,
 * then the refresh
 * job re-prices it by id every week instead of searching again, because search
 * results drift
 * and the first hit isn't always the right cut.
 *
 * An ingredient can have several products at one source, as backups when one is
 * out of stock.
 * Turning a mapping off keeps its price history; deleting the ingredient
 * deletes its mappings.
 */
@Entity
@Table(name = "ingredient_products", uniqueConstraints = @UniqueConstraint(name = "uk_ingredient_products", columnNames = {
        "ingredient_id", "source", "external_id" }))
public class IngredientProduct {

    // ---------- id ----------

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---------- columns ----------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PriceSource source;

    /** The source's id: a Kroger product id, a BLS series id */
    @Column(name = "external_id", nullable = false, updatable = false, length = 100)
    private String externalId;

    /** What the source calls it, so a person can check the mapping makes sense */
    @Column
    private String label;

    @Column(nullable = false)
    private boolean active = true;

    // ---------- timestamps ----------

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ---------- relationships ----------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false, updatable = false)
    private Ingredient ingredient;

    // ---------- constructors ----------

    protected IngredientProduct() {
        // required by JPA
    }

    public IngredientProduct(Ingredient ingredient, PriceSource source, String externalId, String label) {
        this.ingredient = requireNonNull(ingredient, "ingredient");
        this.source = requireNonNull(source, "source");
        this.externalId = requireText(externalId, "externalId");
        this.label = trimToNull(label);
    }

    // ---------- getters ----------

    public UUID getId() {
        return id;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public PriceSource getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // ---------- changes ----------

    public void setLabel(String label) {
        this.label = trimToNull(label);
    }

    /** The refresh job skips inactive products; their price history stays. */
    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}