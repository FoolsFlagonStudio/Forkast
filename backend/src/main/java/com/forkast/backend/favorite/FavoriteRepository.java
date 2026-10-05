package com.forkast.backend.favorite;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    List<Favorite> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndRecipeId(UUID userId, UUID recipeId);

    long deleteByUserIdAndRecipeId(UUID userId, UUID recipeId);

    /**
     * Which of these recipes the user has favorited: one query for a whole page.
     */
    @Query("select f.recipe.id from Favorite f where f.user.id = :userId and f.recipe.id in :recipeIds")
    Set<UUID> findFavoritedRecipeIds(@Param("userId") UUID userId, @Param("recipeIds") Collection<UUID> recipeIds);

    /**
     * Inserts the favorite unless it already exists, in one statement. The unique
     * constraint on
     * (user_id, recipe_id) decides, so two requests racing can't both insert.
     */
    @Modifying
    @Query(value = """
            insert into favorites (id, user_id, recipe_id, created_at)
            values (gen_random_uuid(), :userId, :recipeId, now())
            on conflict (user_id, recipe_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("recipeId") UUID recipeId);

    @Query("select f.recipe.id from Favorite f where f.user.id = :userId order by f.createdAt desc")
    List<UUID> findRecipeIdsNewestFirst(@Param("userId") UUID userId);
}