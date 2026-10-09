package com.forkast.backend.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientPrice;
import com.forkast.backend.ingredient.IngredientPriceRepository;
import com.forkast.backend.ingredient.IngredientProduct;
import com.forkast.backend.ingredient.IngredientProductRepository;
import com.forkast.backend.ingredient.IngredientRepository;
import com.forkast.backend.ingredient.PriceSource;
import com.forkast.backend.ingredient.SoldBy;

/**
 * A product switched off stops counting, even when it was the cheapest. Rolls
 * back.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class CurrentPriceServiceIntegrationTest {

    @Autowired
    private CurrentPriceService currentPriceService;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private IngredientPriceRepository priceRepository;
    @Autowired
    private IngredientProductRepository productRepository;

    private void price(Ingredient ingredient, String productId, String per100g) {
        priceRepository.saveAndFlush(
                IngredientPrice.builder(ingredient, PriceSource.KROGER, new BigDecimal("1.00"), BigDecimal.ONE, "lb")
                        .recordedAt(Instant.now())
                        .product(productId, "1 lb", SoldBy.UNIT)
                        .unitPrices(new BigDecimal(per100g), null)
                        .build());
    }

    @Test
    void switchedOffProductStopsCounting() {
        Ingredient lemon = ingredientRepository.saveAndFlush(new Ingredient("switch test lemon"));
        IngredientProduct soda = new IngredientProduct(lemon, PriceSource.KROGER, "soda", "Lemon Lime Soda 2 liter");
        IngredientProduct lemons = new IngredientProduct(lemon, PriceSource.KROGER, "lemons", "Lemons");
        productRepository.saveAndFlush(soda);
        productRepository.saveAndFlush(lemons);
        price(lemon, "soda", "0.0558");
        price(lemon, "lemons", "0.4500");

        assertThat(currentPriceService.currentPrices(List.of(lemon.getId())).get(lemon.getId()).per100g())
                .isEqualByComparingTo("0.0558"); // the wrong pick wins while it's on

        soda.deactivate();
        productRepository.saveAndFlush(soda);

        assertThat(currentPriceService.currentPrices(List.of(lemon.getId())).get(lemon.getId()).per100g())
                .isEqualByComparingTo("0.4500");
    }
}