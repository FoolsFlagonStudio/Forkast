package com.forkast.backend.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.recipe.RecipeRepository;
import com.forkast.backend.recipe.RecipeStep;

import jakarta.persistence.EntityManager;

/**
 * Checks that V8 and the entities agree: the new columns save and read back,
 * the new source
 * values pass the check constraint, and a product can't be mapped twice. Uses
 * the seeded
 * "onion" ingredient. Everything rolls back.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class PricingSchemaIntegrationTest {

    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private IngredientPriceRepository priceRepository;
    @Autowired
    private IngredientProductRepository productRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private EntityManager entityManager;

    private Ingredient onion() {
        return ingredientRepository.findByName("onion").orElseThrow();
    }

    @Test
    void krogerPriceRoundTripsWithUnitPrices() {
        Ingredient onion = onion();
        // The 3 lb bag from the step 1 test run: $3.79, sold as one package
        IngredientPrice saved = priceRepository.saveAndFlush(
                IngredientPrice.builder(onion, PriceSource.KROGER, new BigDecimal("3.79"), new BigDecimal("3"), "lb")
                        .storeName("01400513")
                        .product("0001111091682", "3 lb", SoldBy.UNIT)
                        .unitPrices(new BigDecimal("0.2785"), null)
                        .build());
        entityManager.clear();

        IngredientPrice read = priceRepository.findById(saved.getId()).orElseThrow();

        assertThat(read.getSource()).isEqualTo(PriceSource.KROGER);
        assertThat(read.getSoldBy()).isEqualTo(SoldBy.UNIT);
        assertThat(read.getSizeText()).isEqualTo("3 lb");
        assertThat(read.getExternalId()).isEqualTo("0001111091682");
        assertThat(read.getPricePer100g()).isEqualByComparingTo("0.2785");
        assertThat(read.getPricePerItem()).isNull();
        assertThat(read.getPromoPrice()).isNull();
        assertThat(read.hasUnitPrice()).isTrue();
    }

    @Test
    void everyNewSourcePassesTheCheckConstraint() {
        Ingredient onion = onion();
        for (PriceSource source : PriceSource.values()) {
            priceRepository.saveAndFlush(
                    IngredientPrice.builder(onion, source, new BigDecimal("1.29"), BigDecimal.ONE, "lb").build());
        }
    }

    @Test
    void sameProductCantBeMappedTwice() {
        Ingredient onion = onion();
        productRepository
                .saveAndFlush(new IngredientProduct(onion, PriceSource.KROGER, "0000000004093", "Onions - Yellow"));
        productRepository.saveAndFlush(
                new IngredientProduct(onion, PriceSource.KROGER, "0001111091682", "Kroger Yellow Onion 3 lb Bag"));

        assertThat(productRepository.findActiveWithIngredient(PriceSource.KROGER))
                .extracting(IngredientProduct::getExternalId)
                .contains("0000000004093", "0001111091682");

        assertThatThrownBy(() -> productRepository.saveAndFlush(
                new IngredientProduct(onion, PriceSource.KROGER, "0000000004093", "duplicate")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void recipeCostRoundTrips() {
        Recipe recipe = new Recipe("Pricing Test Soup", 4);
        recipe.addStep(new RecipeStep(1, "Simmer."));
        recipe.setCost(new BigDecimal("2.35"), true);
        Recipe saved = recipeRepository.saveAndFlush(recipe);
        entityManager.clear();

        Recipe read = recipeRepository.findById(saved.getId()).orElseThrow();

        assertThat(read.getCostPerServing()).isEqualByComparingTo("2.35");
        assertThat(read.isCostComplete()).isTrue();
    }
}