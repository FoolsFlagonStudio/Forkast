package com.forkast.backend.favorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.common.exception.ApiException;
import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.recipe.RecipeQueryService;
import com.forkast.backend.recipe.RecipeStep;
import com.forkast.backend.recipe.RecipeRepository;
import com.forkast.backend.recipe.search.RecipeSummaryResponse;
import com.forkast.backend.user.UserRepository;

/**
 * Favorites against the real database, as the other@example.com test account. A
 * throwaway
 * recipe with no labels is used, so the test only works while that account has
 * no dietary
 * restrictions. Everything rolls back.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class FavoriteServiceIntegrationTest {

    @Autowired
    private FavoriteService favoriteService;
    @Autowired
    private RecipeQueryService recipeQueryService;
    @Autowired
    private FavoriteRepository favoriteRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private UserRepository userRepository;

    private UUID userId;
    private UUID recipeId;

    @BeforeEach
    void setUp() {
        userId = userRepository.findByEmail("other@example.com").orElseThrow().getId();
        Recipe recipe = new Recipe("Favorite Test Toast", 1);
        recipe.addStep(new RecipeStep(1, "Toast it."));
        recipeId = recipeRepository.saveAndFlush(recipe).getId();
    }

    @Test
    void favoritingTwiceLeavesOneRow() {
        favoriteService.add(userId, recipeId);
        favoriteService.add(userId, recipeId);

        assertThat(favoriteRepository.findRecipeIdsNewestFirst(userId))
                .filteredOn(recipeId::equals)
                .hasSize(1);
    }

    @Test
    void removingTwiceIsFine() {
        favoriteService.add(userId, recipeId);

        favoriteService.remove(userId, recipeId);
        favoriteService.remove(userId, recipeId);

        assertThat(favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId)).isFalse();
    }

    @Test
    void favoritesListShowsTheRecipeAsFavorited() {
        favoriteService.add(userId, recipeId);

        RecipeSummaryResponse first = recipeQueryService.favorites(userId, 0, 20).items().get(0);

        assertThat(first.id()).isEqualTo(recipeId); // newest first
        assertThat(first.favorite()).isTrue();
    }

    @Test
    void unknownRecipeIsNotFound() {
        assertThatThrownBy(() -> favoriteService.add(userId, UUID.randomUUID()))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}