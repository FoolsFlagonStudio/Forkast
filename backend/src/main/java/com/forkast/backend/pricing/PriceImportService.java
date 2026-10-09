package com.forkast.backend.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientPrice;
import com.forkast.backend.ingredient.IngredientPriceRepository;
import com.forkast.backend.ingredient.PriceSource;

/**
 * Stores prices sent by import_prices.py (BLS and the seed CSV). Each price is checked on its
 * own: an unknown ingredient or an unreadable size is listed in "failed" and the rest still
 * import.
 *
 * Rerunning an import is safe. A price with a date (a BLS month) is skipped when that month is
 * already stored. A price without one (the seed CSV) is skipped when it matches the latest
 * stored price for that ingredient, so only edited rows add history.
 */
@Service
public class PriceImportService {

    private static final Set<PriceSource> IMPORTABLE = Set.of(
            PriceSource.BLS, PriceSource.SEED, PriceSource.MANUAL, PriceSource.OPEN_PRICES);

    private final IngredientLookup ingredientLookup;
    private final IngredientPriceRepository priceRepository;
    private final UnitPriceCalculator unitPriceCalculator;

    public PriceImportService(IngredientLookup ingredientLookup,
            IngredientPriceRepository priceRepository,
            UnitPriceCalculator unitPriceCalculator) {
        this.ingredientLookup = ingredientLookup;
        this.priceRepository = priceRepository;
        this.unitPriceCalculator = unitPriceCalculator;
    }

    @Transactional
    public PriceImportResult importAll(List<PriceImportRequest> requests) {
        int imported = 0;
        int skipped = 0;
        List<PriceImportResult.Failure> failed = new ArrayList<>();

        for (PriceImportRequest request : requests) {
            if (!IMPORTABLE.contains(request.source())) {
                failed.add(new PriceImportResult.Failure(request.ingredient(),
                        request.source() + " prices come from the refresh job, not an import"));
                continue;
            }
            Optional<Ingredient> ingredient = ingredientLookup.find(request.ingredient());
            if (ingredient.isEmpty()) {
                failed.add(new PriceImportResult.Failure(request.ingredient(), "No ingredient with that name or alias"));
                continue;
            }
            Optional<PackageSize> size = PackageSizeParser.parse(request.size());
            if (size.isEmpty()) {
                failed.add(new PriceImportResult.Failure(request.ingredient(),
                        "Can't read the size '" + request.size() + "'"));
                continue;
            }
            if (alreadyStored(ingredient.get(), request)) {
                skipped++;
                continue;
            }

            UnitPrice unitPrice = unitPriceCalculator.calculate(request.price(), size.get(),
                    request.averageGramsPerItem(), ingredient.get());
            if (!unitPrice.isKnown()) {
                failed.add(new PriceImportResult.Failure(request.ingredient(),
                        "Can't convert '" + request.size() + "' to grams for this ingredient (no portion for that unit)"));
                continue;
            }

            priceRepository.save(IngredientPrice.builder(ingredient.get(), request.source(),
                    request.price(), size.get().quantity(), size.get().unit().name().toLowerCase(Locale.ROOT))
                    .recordedAt(request.recordedAt() != null ? request.recordedAt() : Instant.now())
                    .storeName(request.storeName())
                    .product(request.externalId(), request.size(), null)
                    .unitPrices(unitPrice.per100g(), unitPrice.perItem())
                    .build());
            imported++;
        }
        return new PriceImportResult(imported, skipped, failed, 0);
    }

    private boolean alreadyStored(Ingredient ingredient, PriceImportRequest request) {
        String externalId = blankToNull(request.externalId());
        if (request.recordedAt() != null) {
            return priceRepository.existsByIngredientIdAndSourceAndExternalIdAndRecordedAt(
                    ingredient.getId(), request.source(), externalId, request.recordedAt());
        }
        return priceRepository.findFirstByIngredientIdAndSourceAndExternalIdOrderByRecordedAtDesc(
                ingredient.getId(), request.source(), externalId)
                .filter(latest -> samePrice(latest.getPrice(), request.price())
                        && request.size().trim().equals(latest.getSizeText()))
                .isPresent();
    }

    private static boolean samePrice(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) == 0;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}