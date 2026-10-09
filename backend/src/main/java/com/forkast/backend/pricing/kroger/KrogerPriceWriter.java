package com.forkast.backend.pricing.kroger;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.ingest.Unit;
import com.forkast.backend.ingredient.IngredientPrice;
import com.forkast.backend.ingredient.IngredientPriceRepository;
import com.forkast.backend.ingredient.IngredientProduct;
import com.forkast.backend.ingredient.IngredientProductRepository;
import com.forkast.backend.ingredient.PriceSource;
import com.forkast.backend.ingredient.SoldBy;
import com.forkast.backend.pricing.PackageSize;
import com.forkast.backend.pricing.PackageSizeParser;
import com.forkast.backend.pricing.UnitPrice;
import com.forkast.backend.pricing.UnitPriceCalculator;

/**
 * Stores one Kroger quote as a price row, in its own short transaction. The
 * HTTP call happens
 * before this, outside any transaction, so a slow Kroger response never holds a
 * database
 * connection.
 *
 * By-weight items are priced per pound whatever the size text says, so their
 * size is taken as
 * 1 lb. Counted packages ("1 ct", "12 ct") use the package weight Kroger gives,
 * never the
 * catalog's portions (see UnitPriceCalculator.forCountPackage). A size that
 * can't be read is still stored (for the record) with no unit price, and the
 * resolver skips it.
 */
@Service
public class KrogerPriceWriter {

        private static final PackageSize ONE_POUND = new PackageSize(BigDecimal.ONE, Unit.LB);

        private final IngredientProductRepository productRepository;
        private final IngredientPriceRepository priceRepository;
        private final UnitPriceCalculator unitPriceCalculator;

        public KrogerPriceWriter(IngredientProductRepository productRepository,
                        IngredientPriceRepository priceRepository,
                        UnitPriceCalculator unitPriceCalculator) {
                this.productRepository = productRepository;
                this.priceRepository = priceRepository;
                this.unitPriceCalculator = unitPriceCalculator;
        }

        /** Returns true when the price has a unit price the resolver can use. */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public boolean save(UUID mappingId, KrogerQuote quote, String storeName) {
                IngredientProduct mapping = productRepository.findById(mappingId)
                                .orElseThrow(() -> new IllegalStateException("Mapping " + mappingId + " is gone"));

                Optional<PackageSize> size = quote.soldBy() == SoldBy.WEIGHT
                                ? Optional.of(ONE_POUND)
                                : PackageSizeParser.parse(quote.size());
                UnitPrice unitPrice = size
                                .map(s -> s.unit() == Unit.COUNT
                                                ? UnitPriceCalculator.forCountPackage(quote.regular(), s.quantity(),
                                                                quote.packageGrams())
                                                : unitPriceCalculator.calculate(quote.regular(), s,
                                                                quote.averageGramsPerItem(),
                                                                mapping.getIngredient()))
                                .orElse(UnitPrice.NONE);

                BigDecimal quantity = size.map(PackageSize::quantity).orElse(BigDecimal.ONE);
                String unit = size.map(s -> s.unit().name().toLowerCase(Locale.ROOT)).orElse("package");

                priceRepository.save(IngredientPrice.builder(mapping.getIngredient(), PriceSource.KROGER,
                                quote.regular(), quantity, unit)
                                .storeName(storeName)
                                .promoPrice(quote.promo())
                                .product(quote.productId(), quote.size(), quote.soldBy())
                                .unitPrices(unitPrice.per100g(), unitPrice.perItem())
                                .build());

                if (mapping.getLabel() == null && quote.label() != null) {
                        mapping.setLabel(quote.label());
                }
                return unitPrice.isKnown();
        }
}