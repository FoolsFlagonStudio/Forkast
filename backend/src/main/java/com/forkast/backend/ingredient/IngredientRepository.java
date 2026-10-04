package com.forkast.backend.ingredient;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    Optional<Ingredient> findByFdcId(Long fdcId);

    Optional<Ingredient> findByNameIgnoreCase(String name);

    /** Names are stored lowercase, so the matcher can use a plain equals lookup. */
    Optional<Ingredient> findByName(String name);

    /** Best trigram similarity across every ingredient name and alias. */
    @Query(value = """
            select hit.ingredient_id as "ingredientId", max(hit.score) as "score"
            from (
                select i.id as ingredient_id,
                       cast(extensions.similarity(i.name, :text) as double precision) as score
                from ingredients i
                union all
                select a.ingredient_id,
                       cast(extensions.similarity(a.alias, :text) as double precision)
                from ingredient_aliases a
            ) hit
            group by hit.ingredient_id
            order by max(hit.score) desc
            limit 1
            """, nativeQuery = true)
    Optional<SimilarityHit> findMostSimilar(@Param("text") String text);
}