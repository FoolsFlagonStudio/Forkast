package com.forkast.backend.recipe;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeRepository extends JpaRepository<Recipe, UUID>, JpaSpecificationExecutor<Recipe> {

    List<Recipe> findByNameContainingIgnoreCase(String text);

    boolean existsBySourceUrl(String sourceUrl);

    /**
     * Loads a page of recipes with their labels in one query, instead of one query
     * per recipe.
     */
    @Query("select distinct r from Recipe r left join fetch r.dietaryLabels where r.id in :ids")
    List<Recipe> findWithLabelsByIdIn(@Param("ids") Collection<UUID> ids);

    @Query("select r from Recipe r left join fetch r.ingredients ri left join fetch ri.ingredient where r.id = :id")
    Optional<Recipe> findWithIngredientsById(@Param("id") UUID id);
}