package com.forkast.backend.recipe;

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

import jakarta.persistence.EntityManager;

/**
 * Builds a throwaway recipe and reads it back through the detail query. A
 * random user id has
 * no preferences, so there are no restrictions and the default servings are the
 * recipe's own.
 * Everything rolls back.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class RecipeDetailIntegrationTest {

    @Autowired
    private RecipeQueryService queryService;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private EntityManager entityManager;

    private Recipe saveTestRecipe() {
        Recipe recipe = new Recipe("Detail Test Soup", 6);
        recipe.addStep(new RecipeStep(1, "Chop."));
        recipe.addStep(new RecipeStep(2, "Simmer."));
        recipe.addIngredient(line("1 (15 oz) can black beans", "15", "oz", "black beans"));
        recipe.addIngredient(line("3 cups water", "3", "cup", "water"));
        recipe.addIngredient(line("Salt to taste", null, "to_taste", "salt"));
        return recipeRepository.saveAndFlush(recipe);
    }

    private static RecipeIngredient line(String raw, String amount, String unit, String parsedName) {
        RecipeIngredient line = new RecipeIngredient(raw, amount == null ? null : new BigDecimal(amount), unit);
        line.setParsedName(parsedName);
        return line;
    }

    @Test
    void ingredientsComeBackInRecipeOrderAndScaled() {
        Recipe recipe = saveTestRecipe();
        // Forget the in-memory recipe so the detail query reads the order back from the
        // database.
        entityManager.clear();

        RecipeDetailResponse detail = queryService.detail(UUID.randomUUID(), recipe.getId(), 2);

        assertThat(detail.servings()).isEqualTo(2);
        assertThat(detail.ingredients()).extracting(RecipeDetailResponse.IngredientLine::name)
                .containsExactly("black beans", "water", "salt");
        assertThat(detail.ingredients()).extracting(RecipeDetailResponse.IngredientLine::displayAmount)
                .containsExactly("5", "1", null); // 15 oz / 3, 3 cups / 3, to taste unchanged
        assertThat(detail.steps()).extracting(RecipeDetailResponse.Step::text).containsExactly("Chop.", "Simmer.");
    }

    @Test
    void noServingsAndNoPreferencesUsesTheRecipesOwn() {
        Recipe recipe = saveTestRecipe();

        RecipeDetailResponse detail = queryService.detail(UUID.randomUUID(), recipe.getId(), null);

        assertThat(detail.servings()).isEqualTo(6);
        assertThat(detail.ingredients().get(1).displayAmount()).isEqualTo("3");
    }

    @Test
    void unknownRecipeIsNotFound() {
        assertThatThrownBy(() -> queryService.detail(UUID.randomUUID(), UUID.randomUUID(), null))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}