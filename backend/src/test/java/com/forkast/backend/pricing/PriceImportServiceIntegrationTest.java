package com.forkast.backend.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientPortion;
import com.forkast.backend.ingredient.IngredientPrice;
import com.forkast.backend.ingredient.IngredientPriceRepository;
import com.forkast.backend.ingredient.IngredientRepository;
import com.forkast.backend.ingredient.PriceSource;

/**
 * Imports against the real database with throwaway ingredients made here. Calls
 * the service,
 * not the controller, so the cost pass (which commits in its own transactions)
 * doesn't run.
 * Everything rolls back.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class PriceImportServiceIntegrationTest {

    private static final Instant AUGUST = Instant.parse("2026-09-01T00:00:00Z");

    @Autowired
    private PriceImportService importService;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private IngredientPriceRepository priceRepository;

    private Ingredient onion;
    private Ingredient eggs;

    @BeforeEach
    void setUp() {
        onion = new Ingredient("import test onion");
        onion.addAlias("import test yellow onion");
        onion = ingredientRepository.saveAndFlush(onion);

        eggs = new Ingredient("import test egg");
        eggs.addPortion(new IngredientPortion("large", new BigDecimal("50")));
        eggs = ingredientRepository.saveAndFlush(eggs);
    }

    private static PriceImportRequest bls(String ingredient, String price, String size, Instant month) {
        return new PriceImportRequest(ingredient, PriceSource.BLS, new BigDecimal(price), size,
                "APU0000TEST", month, "BLS U.S. city average", null);
    }

    private static PriceImportRequest seed(String ingredient, String price, String size) {
        return new PriceImportRequest(ingredient, PriceSource.SEED, new BigDecimal(price), size,
                null, null, null, null);
    }

    private List<IngredientPrice> pricesOf(Ingredient ingredient) {
        return priceRepository.findByIngredientIdOrderByRecordedAtDesc(ingredient.getId());
    }

    @Test
    void storesTheUnitPrice() {
        PriceImportResult result = importService.importAll(List.of(
                bls("import test onion", "1.36", "1 lb", AUGUST),
                bls("import test egg", "3.49", "1 dozen", AUGUST)));

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.failed()).isEmpty();

        IngredientPrice onionPrice = pricesOf(onion).get(0);
        assertThat(onionPrice.getSource()).isEqualTo(PriceSource.BLS);
        assertThat(onionPrice.getExternalId()).isEqualTo("APU0000TEST");
        assertThat(onionPrice.getRecordedAt()).isEqualTo(AUGUST);
        assertThat(onionPrice.getUnit()).isEqualTo("lb");
        assertThat(onionPrice.getPricePer100g()).isEqualByComparingTo("0.2998"); // 1.36 / 453.59 g

        IngredientPrice eggPrice = pricesOf(eggs).get(0);
        assertThat(eggPrice.getQuantity()).isEqualByComparingTo("12");
        assertThat(eggPrice.getUnit()).isEqualTo("count");
        assertThat(eggPrice.getPricePerItem()).isEqualByComparingTo("0.2908");
    }

    @Test
    void findsIngredientsByAlias() {
        PriceImportResult result = importService.importAll(List.of(
                bls("Import Test Yellow Onion", "1.36", "1 lb", AUGUST)));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(pricesOf(onion)).hasSize(1);
    }

    @Test
    void sameBlsMonthTwiceIsSkipped() {
        importService.importAll(List.of(bls("import test onion", "1.36", "1 lb", AUGUST)));

        PriceImportResult again = importService.importAll(List.of(
                bls("import test onion", "1.36", "1 lb", AUGUST),
                bls("import test onion", "1.41", "1 lb", Instant.parse("2026-10-01T00:00:00Z"))));

        assertThat(again.skipped()).isEqualTo(1);
        assertThat(again.imported()).isEqualTo(1); // the new month
        assertThat(pricesOf(onion)).hasSize(2);
    }

    @Test
    void unchangedSeedPriceIsSkippedButAnEditedOneIsStored() {
        importService.importAll(List.of(seed("import test onion", "1.29", "1 lb")));

        PriceImportResult same = importService.importAll(List.of(seed("import test onion", "1.29", "1 lb")));
        PriceImportResult edited = importService.importAll(List.of(seed("import test onion", "1.49", "1 lb")));

        assertThat(same.skipped()).isEqualTo(1);
        assertThat(edited.imported()).isEqualTo(1);
        assertThat(pricesOf(onion)).hasSize(2);
    }

    @Test
    void problemsAreListedAndTheRestStillImport() {
        PriceImportResult result = importService.importAll(List.of(
                seed("no such ingredient", "1.00", "1 lb"),
                seed("import test onion", "1.00", "family size"),
                seed("import test onion", "1.00", "1 gal"), // no portion to weigh a gallon of onion
                new PriceImportRequest("import test onion", PriceSource.KROGER, BigDecimal.ONE, "1 lb",
                        null, null, null, null),
                seed("import test egg", "3.49", "12 ct")));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.failed()).extracting(PriceImportResult.Failure::reason)
                .hasSize(4)
                .anyMatch(r -> r.startsWith("No ingredient"))
                .anyMatch(r -> r.startsWith("Can't read the size"))
                .anyMatch(r -> r.startsWith("Can't convert"))
                .anyMatch(r -> r.contains("refresh job"));
    }
}