package com.forkast.backend.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientProduct;
import com.forkast.backend.ingredient.IngredientProductRepository;
import com.forkast.backend.ingredient.IngredientRepository;
import com.forkast.backend.ingredient.PriceSource;

/**
 * Mapping import against the real database, with a throwaway ingredient. Rolls
 * back.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class ProductMappingServiceIntegrationTest {

    @Autowired
    private ProductMappingService mappingService;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private IngredientProductRepository productRepository;

    private Ingredient onion;

    @BeforeEach
    void setUp() {
        onion = ingredientRepository.saveAndFlush(new Ingredient("mapping test onion"));
    }

    private static ProductMappingRequest kroger(String ingredient, String productId, String label, Boolean active) {
        return new ProductMappingRequest(ingredient, PriceSource.KROGER, productId, label, active);
    }

    private List<IngredientProduct> mappings() {
        return productRepository.findByIngredientIdAndSource(onion.getId(), PriceSource.KROGER);
    }

    @Test
    void createsThenRecognisesTheSameRow() {
        ProductMappingResult first = mappingService.importAll(List.of(
                kroger("mapping test onion", "0000000004093", "Onions - Yellow", null),
                kroger("mapping test onion", "0001111091682", "Kroger Yellow Onion 3 lb Bag", null)));
        ProductMappingResult again = mappingService.importAll(List.of(
                kroger("mapping test onion", "0000000004093", "Onions - Yellow", null)));

        assertThat(first.created()).isEqualTo(2);
        assertThat(again.unchanged()).isEqualTo(1);
        assertThat(mappings()).hasSize(2).allMatch(IngredientProduct::isActive);
    }

    @Test
    void updatesLabelAndCanTurnAProductOff() {
        mappingService.importAll(List.of(kroger("mapping test onion", "0000000004093", "Onions", null)));

        ProductMappingResult result = mappingService.importAll(List.of(
                kroger("mapping test onion", "0000000004093", "Onions - Yellow", false)));

        assertThat(result.updated()).isEqualTo(1);
        IngredientProduct mapping = mappings().get(0);
        assertThat(mapping.getLabel()).isEqualTo("Onions - Yellow");
        assertThat(mapping.isActive()).isFalse();
        assertThat(productRepository.findActiveWithIngredient(PriceSource.KROGER))
                .noneMatch(p -> p.getId().equals(mapping.getId()));
    }

    @Test
    void unknownIngredientIsListed() {
        ProductMappingResult result = mappingService.importAll(List.of(
                kroger("no such ingredient", "123", null, null)));

        assertThat(result.failed()).hasSize(1);
        assertThat(result.created()).isZero();
    }
}