package com.forkast.backend.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.ingest.RecipeEnricher;
import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientPrice;
import com.forkast.backend.ingredient.IngredientPriceRepository;
import com.forkast.backend.ingredient.IngredientRepository;
import com.forkast.backend.ingredient.PriceSource;
import com.forkast.backend.ingredient.SoldBy;
import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.recipe.RecipeIngredient;
import com.forkast.backend.recipe.RecipeRepository;
import com.forkast.backend.recipe.RecipeStep;

/**
 * The enricher prices a recipe from what's in the database. The ingredient, its
 * prices and
 * the recipe are all made here, so real prices loaded later can't change the
 * result.
 * Everything rolls back.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class RecipeCostIntegrationTest {

    @Autowired
    private RecipeEnricher enricher;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private IngredientPriceRepository priceRepository;
    @Autowired
    private RecipeRepository recipeRepository;

    private Ingredient testOnion() {
        return ingredientRepository.saveAndFlush(new Ingredient("cost test onion"));
    }

    private Recipe onionRecipe(Ingredient onion) {
        Recipe recipe = new Recipe("Cost Test Onions", 2);
        recipe.addStep(new RecipeStep(1, "Cook the onions."));
        RecipeIngredient line = new RecipeIngredient("3 lb onions", new BigDecimal("3"), "lb");
        line.matchIngredient(onion, BigDecimal.ONE);
        recipe.addIngredient(line);
        return recipeRepository.saveAndFlush(recipe);
    }

    private void savePrice(Ingredient onion, PriceSource source, String per100g, Instant recordedAt) {
        priceRepository.saveAndFlush(
                IngredientPrice.builder(onion, source, new BigDecimal("3.79"), new BigDecimal("3"), "lb")
                        .recordedAt(recordedAt)
                        .product("cost-test-" + source, "3 lb", SoldBy.UNIT)
                        .unitPrices(new BigDecimal(per100g), null)
                        .build());
    }

    @Test
    void enrichPricesTheRecipeFromTheCurrentPrice() {
        Ingredient onion = testOnion();
        // an old seed price and a current Kroger one: Kroger wins
        savePrice(onion, PriceSource.SEED, "0.5000", Instant.now().minus(Duration.ofDays(300)));
        savePrice(onion, PriceSource.KROGER, "0.2785", Instant.now().minus(Duration.ofDays(1)));
        Recipe recipe = onionRecipe(onion);

        enricher.enrich(recipe);

        // 3 lb = 1360.77 g x $0.2785 / 100 = $3.79, over 2 servings
        assertThat(recipe.getCostPerServing()).isEqualByComparingTo("1.89");
        assertThat(recipe.isCostComplete()).isTrue();
    }

    @Test
    void updateCostChangesOnlyTheCost() {
        Ingredient onion = testOnion();
        Recipe recipe = onionRecipe(onion);
        enricher.enrich(recipe);
        int mealPrepScore = recipe.getMealPrepScore();

        savePrice(onion, PriceSource.KROGER, "0.5570", Instant.now()); // the price doubled
        enricher.updateCost(recipe);

        assertThat(recipe.getCostPerServing()).isEqualByComparingTo("3.79");
        assertThat(recipe.getMealPrepScore()).isEqualTo(mealPrepScore);
    }
}