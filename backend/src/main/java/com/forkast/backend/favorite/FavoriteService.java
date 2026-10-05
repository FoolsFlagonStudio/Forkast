package com.forkast.backend.favorite;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.recipe.RecipeQueryService;

/**
 * Adds and removes favorites. Both are idempotent: favoriting twice leaves one
 * row, and
 * removing something that isn't a favorite is fine, so the app can safely retry
 * either.
 */
@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final RecipeQueryService recipeQueryService;

    public FavoriteService(FavoriteRepository favoriteRepository, RecipeQueryService recipeQueryService) {
        this.favoriteRepository = favoriteRepository;
        this.recipeQueryService = recipeQueryService;
    }

    @Transactional
    public void add(UUID userId, UUID recipeId) {
        // 404 for a missing recipe, or one the user's restrictions exclude
        recipeQueryService.requireVisible(userId, recipeId);

        // One statement that skips the insert if the row exists. Two requests racing
        // can't
        // both insert, and there's no exception to catch: Postgres resolves it.
        favoriteRepository.insertIfAbsent(userId, recipeId);
    }

    @Transactional
    public void remove(UUID userId, UUID recipeId) {
        favoriteRepository.deleteByUserIdAndRecipeId(userId, recipeId);
    }
}