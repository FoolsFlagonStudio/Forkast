package com.forkast.backend.ingredient;

import static com.forkast.backend.common.ValidationUtils.requireNonNull;
import static com.forkast.backend.common.ValidationUtils.requirePositive;
import static com.forkast.backend.common.ValidationUtils.requireText;
import static com.forkast.backend.common.ValidationUtils.trimToNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredient_prices", indexes = @Index(name = "idx_price_ingredient_recorded", columnList = "ingredient_id, recorded_at"))
public class IngredientPrice {

    // ---------- id ----------

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---------- columns ----------

    @Column(name = "store_name", updatable = false)
    private String storeName;

    @Column(nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, updatable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, updatable = false, length = 50)
    private String unit;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PriceSource source;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    // ---------- relationships ----------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false, updatable = false)
    private Ingredient ingredient;

    // ---------- constructors ----------

    protected IngredientPrice() {
    }

    public IngredientPrice(Ingredient ingredient,
            BigDecimal price,
            BigDecimal quantity,
            String unit,
            String currency,
            PriceSource source,
            Instant recordedAt,
            String storeName) {
        this.ingredient = requireNonNull(ingredient, "ingredient");
        this.price = requirePositive(price, "price");
        this.quantity = requirePositive(quantity, "quantity");
        this.unit = requireText(unit, "unit");
        this.currency = requireCurrency(currency);
        this.source = requireNonNull(source, "source");
        this.recordedAt = requireNonNull(recordedAt, "recordedAt");
        this.storeName = trimToNull(storeName);
    }

    /** Convenience for prices a user enters by hand right now. */
    public static IngredientPrice manual(Ingredient ingredient,
            BigDecimal price,
            BigDecimal quantity,
            String unit,
            String storeName) {
        return new IngredientPrice(ingredient, price, quantity, unit,
                "USD", PriceSource.MANUAL, Instant.now(), storeName);
    }

    // ---------- getters only (immutable) ----------

    public UUID getId() {
        return id;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public String getStoreName() {
        return storeName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public String getCurrency() {
        return currency;
    }

    public PriceSource getSource() {
        return source;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    // ---------- private helpers ----------

    private static String requireCurrency(String currency) {
        String code = requireText(currency, "currency").toUpperCase();
        if (code.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter code, e.g. USD");
        }
        return code;
    }
}