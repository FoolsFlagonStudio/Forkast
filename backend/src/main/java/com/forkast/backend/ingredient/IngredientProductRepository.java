package com.forkast.backend.ingredient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IngredientProductRepository extends JpaRepository<IngredientProduct, UUID> {

        /**
         * What the refresh job re-prices for one source. The ingredient is fetched in
         * the same
         * query because every saved price needs it, which would otherwise be one query
         * per row.
         */
        @Query("""
                        select p from IngredientProduct p
                        join fetch p.ingredient
                        where p.source = :source and p.active = true
                        order by p.ingredient.name, p.createdAt
                        """)
        List<IngredientProduct> findActiveWithIngredient(@Param("source") PriceSource source);

        List<IngredientProduct> findByIngredientIdAndSource(UUID ingredientId, PriceSource source);

        /**
         * Products switched off, so their past prices stop counting
         * (CurrentPriceService).
         */
        List<IngredientProduct> findByActiveFalse();

        Optional<IngredientProduct> findByIngredientIdAndSourceAndExternalId(UUID ingredientId, PriceSource source,
                        String externalId);
}