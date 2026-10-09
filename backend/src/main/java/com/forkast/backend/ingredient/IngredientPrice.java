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

/**
 * One price observation, never changed after it's saved: a new price is a new
 * row, so the
 * table is the price history.
 *
 * price, quantity and unit are what the source said ($3.79 for 3 lb).
 * pricePer100g and
 * pricePerItem are that price in the units recipes need, computed once when the
 * row is built
 * (step 3); both are null when the package size couldn't be read, and the row
 * is then kept
 * for the record but not used for cost.
 *
 * Built with {@link #builder}, because most of the columns are optional and a
 * constructor with
 * thirteen parameters is easy to call in the wrong order.
 */
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

    @Column(name = "promo_price", updatable = false, precision = 10, scale = 2)
    private BigDecimal promoPrice;

    @Column(nullable = false, updatable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, updatable = false, length = 50)
    private String unit;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PriceSource source;

    @Column(name = "external_id", updatable = false, length = 100)
    private String externalId;

    @Column(name = "size_text", updatable = false, length = 100)
    private String sizeText;

    @Enumerated(EnumType.STRING)
    @Column(name = "sold_by", updatable = false, length = 10)
    private SoldBy soldBy;

    @Column(name = "price_per_100g", updatable = false, precision = 10, scale = 4)
    private BigDecimal pricePer100g;

    @Column(name = "price_per_item", updatable = false, precision = 10, scale = 4)
    private BigDecimal pricePerItem;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    // ---------- relationships ----------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false, updatable = false)
    private Ingredient ingredient;

    // ---------- constructors ----------

    protected IngredientPrice() {
        // required by JPA
    }

    private IngredientPrice(Builder b) {
        this.ingredient = requireNonNull(b.ingredient, "ingredient");
        this.source = requireNonNull(b.source, "source");
        this.price = requirePositive(b.price, "price");
        this.quantity = requirePositive(b.quantity, "quantity");
        this.unit = requireText(b.unit, "unit");
        this.currency = requireCurrency(b.currency);
        this.recordedAt = requireNonNull(b.recordedAt, "recordedAt");
        this.storeName = trimToNull(b.storeName);
        this.promoPrice = b.promoPrice == null ? null : requirePositive(b.promoPrice, "promoPrice");
        this.externalId = trimToNull(b.externalId);
        this.sizeText = trimToNull(b.sizeText);
        this.soldBy = b.soldBy;
        this.pricePer100g = requireNonNegative(b.pricePer100g, "pricePer100g");
        this.pricePerItem = requireNonNegative(b.pricePerItem, "pricePerItem");
    }

    /**
     * Starts a price with the fields every row needs: what was priced, where the
     * price came
     * from, and what it bought ($3.79 for 3 lb). Currency defaults to USD and
     * recordedAt to now.
     */
    public static Builder builder(Ingredient ingredient, PriceSource source,
            BigDecimal price, BigDecimal quantity, String unit) {
        return new Builder(ingredient, source, price, quantity, unit);
    }

    /** Convenience for prices a user enters by hand right now. */
    public static IngredientPrice manual(Ingredient ingredient,
            BigDecimal price,
            BigDecimal quantity,
            String unit,
            String storeName) {
        return builder(ingredient, PriceSource.MANUAL, price, quantity, unit)
                .storeName(storeName)
                .build();
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

    public BigDecimal getPromoPrice() {
        return promoPrice;
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

    public String getExternalId() {
        return externalId;
    }

    public String getSizeText() {
        return sizeText;
    }

    public SoldBy getSoldBy() {
        return soldBy;
    }

    public BigDecimal getPricePer100g() {
        return pricePer100g;
    }

    public BigDecimal getPricePerItem() {
        return pricePerItem;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    /** True when this row can be used to cost a recipe. */
    public boolean hasUnitPrice() {
        return pricePer100g != null || pricePerItem != null;
    }

    // ---------- builder ----------

    /**
     * Collects the optional fields one named call at a time, then {@link #build()}
     * validates
     * everything in the entity's constructor. The entity stays immutable: only the
     * builder has
     * setters, and it's thrown away after build().
     */
    public static final class Builder {

        private final Ingredient ingredient;
        private final PriceSource source;
        private final BigDecimal price;
        private final BigDecimal quantity;
        private final String unit;
        private String currency = "USD";
        private Instant recordedAt = Instant.now();
        private String storeName;
        private BigDecimal promoPrice;
        private String externalId;
        private String sizeText;
        private SoldBy soldBy;
        private BigDecimal pricePer100g;
        private BigDecimal pricePerItem;

        private Builder(Ingredient ingredient, PriceSource source,
                BigDecimal price, BigDecimal quantity, String unit) {
            this.ingredient = ingredient;
            this.source = source;
            this.price = price;
            this.quantity = quantity;
            this.unit = unit;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        /**
         * When the price was observed (a BLS month, a Kroger fetch), not when the row
         * is saved.
         */
        public Builder recordedAt(Instant recordedAt) {
            this.recordedAt = recordedAt;
            return this;
        }

        public Builder storeName(String storeName) {
            this.storeName = storeName;
            return this;
        }

        public Builder promoPrice(BigDecimal promoPrice) {
            this.promoPrice = promoPrice;
            return this;
        }

        /**
         * The source's product: its id, the size as written ("3 lb"), and how it's
         * sold.
         */
        public Builder product(String externalId, String sizeText, SoldBy soldBy) {
            this.externalId = externalId;
            this.sizeText = sizeText;
            this.soldBy = soldBy;
            return this;
        }

        /** Either may be null; both null means the size couldn't be read. */
        public Builder unitPrices(BigDecimal pricePer100g, BigDecimal pricePerItem) {
            this.pricePer100g = pricePer100g;
            this.pricePerItem = pricePerItem;
            return this;
        }

        public IngredientPrice build() {
            return new IngredientPrice(this);
        }
    }

    // ---------- private helpers ----------

    private static String requireCurrency(String currency) {
        String code = requireText(currency, "currency").toUpperCase();
        if (code.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter code, e.g. USD");
        }
        return code;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException(field + " can't be negative");
        }
        return value;
    }
}