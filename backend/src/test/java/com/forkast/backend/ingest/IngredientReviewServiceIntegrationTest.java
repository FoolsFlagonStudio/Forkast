package com.forkast.backend.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.common.exception.ApiException;
import com.forkast.backend.diet.DietaryLabel;
import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientAliasRepository;
import com.forkast.backend.ingredient.IngredientRepository;
import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.recipe.RecipeIngredient;
import com.forkast.backend.recipe.RecipeRepository;
import com.forkast.backend.recipe.RecipeStep;

/**
 * Builds two throwaway recipes with the same unmatched line, resolves one line
 * with
 * saveAlias, and checks the other line, the alias and both recipes' nutrition
 * and labels.
 * 
 * @Transactional rolls everything back afterwards, so nothing is left in the
 *                database.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class IngredientReviewServiceIntegrationTest {

    private static final String PARSED_NAME = "forkast test frying fat";

    @Autowired
    private IngredientReviewService reviewService;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private IngredientAliasRepository aliasRepository;
    @Autowired
    private RecipeEnricher enricher;

    @Test
    void resolvingWithAliasMatchesTheQueueAndRecomputesRecipes() {
        Ingredient oil = ingredientRepository.findByName("vegetable oil").orElseThrow();

        Recipe first = recipeWithUnmatchedLine("Review Test One");
        Recipe second = recipeWithUnmatchedLine("Review Test Two");
        RecipeIngredient line = first.getIngredients().get(0);
        RecipeIngredient other = second.getIngredients().get(0);
        assertThat(first.isNutritionComplete()).isFalse();
        assertThat(first.getDietaryLabels()).isEmpty();

        ResolveReviewResponse response = reviewService.resolve(line.getId(),
                new ResolveReviewRequest(oil.getId(), true));

        assertThat(response.ingredientName()).isEqualTo("vegetable oil");
        assertThat(response.alsoMatched()).isEqualTo(1);
        assertThat(other.getIngredient()).isSameAs(oil);
        assertThat(other.isNeedsReview()).isFalse();
        assertThat(aliasRepository.findByAlias(PARSED_NAME)).isPresent();

        // both recipes were recomputed: oil has a tbsp portion, and no restrictive tags
        assertThat(first.isNutritionComplete()).isTrue();
        assertThat(second.isNutritionComplete()).isTrue();
        assertThat(first.getDietaryLabels()).extracting(DietaryLabel::getName).contains("vegan", "gluten-free");
    }

    @Test
    void resolvingWithoutAliasOnlyChangesThatLine() {
        Ingredient oil = ingredientRepository.findByName("vegetable oil").orElseThrow();
        Recipe first = recipeWithUnmatchedLine("Review Test One");
        Recipe second = recipeWithUnmatchedLine("Review Test Two");

        ResolveReviewResponse response = reviewService.resolve(
                first.getIngredients().get(0).getId(), new ResolveReviewRequest(oil.getId(), false));

        assertThat(response.alsoMatched()).isZero();
        assertThat(second.getIngredients().get(0).isNeedsReview()).isTrue();
        assertThat(aliasRepository.findByAlias(PARSED_NAME)).isEmpty();
    }

    @Test
    void unknownLineIsNotFound() {
        Ingredient oil = ingredientRepository.findByName("vegetable oil").orElseThrow();

        assertThatThrownBy(() -> reviewService.resolve(UUID.randomUUID(), new ResolveReviewRequest(oil.getId(), false)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---------- helpers ----------

    private Recipe recipeWithUnmatchedLine(String name) {
        Recipe recipe = new Recipe(name, 2);
        recipe.addStep(new RecipeStep(1, "Cook it."));

        RecipeIngredient line = new RecipeIngredient("2 tbsp " + PARSED_NAME, new BigDecimal("2"), "tbsp");
        line.setParsedName(PARSED_NAME);
        line.flagForReview(new BigDecimal("0.200"));
        recipe.addIngredient(line);

        enricher.enrich(recipe);
        return recipeRepository.saveAndFlush(recipe);
    }
}