package com.forkast.backend.favorite;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    List<Favorite> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndRecipeId(UUID userId, UUID recipeId);

    long deleteByUserIdAndRecipeId(UUID userId, UUID recipeId);
}