package com.forkast.backend.ingredient;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IngredientPriceRepository extends JpaRepository<IngredientPrice, UUID> {

    Optional<IngredientPrice> findFirstByIngredientIdOrderByRecordedAtDesc(UUID ingredientId);

    List<IngredientPrice> findByIngredientIdOrderByRecordedAtDesc(UUID ingredientId);

    /** A null externalId matches rows with no external id (Spring Data turns it into IS NULL). */
    boolean existsByIngredientIdAndSourceAndExternalIdAndRecordedAt(UUID ingredientId, PriceSource source,
            String externalId, Instant recordedAt);

    Optional<IngredientPrice> findFirstByIngredientIdAndSourceAndExternalIdOrderByRecordedAtDesc(UUID ingredientId,
            PriceSource source, String externalId);

    /**
     * Every price for these ingredients, for PriceResolver. Grouping by ingredient reads
     * p.getIngredient().getId(), which Hibernate answers from the lazy proxy without loading
     * the ingredient.
     */
    @Query("select p from IngredientPrice p where p.ingredient.id in :ids")
    List<IngredientPrice> findByIngredientIds(@Param("ids") Collection<UUID> ids);
}