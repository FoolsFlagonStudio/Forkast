package com.forkast.backend.ingest;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.forkast.backend.recipe.RecipeIngredient;

/** Queries the review queue and re-processing need. */
public interface ReviewLineRepository extends JpaRepository<RecipeIngredient, UUID> {

    /** Lines that need review, grouped by parsed name so repeats sit together. */
    @Query(value = """
            select new com.forkast.backend.ingest.ReviewLineResponse(
                ri.id, r.id, r.name, ri.rawText, ri.parsedName, ri.matchScore)
            from RecipeIngredient ri
            join ri.recipe r
            where ri.needsReview = true
            order by ri.parsedName, r.name
            """, countQuery = "select count(ri) from RecipeIngredient ri where ri.needsReview = true")
    Page<ReviewLineResponse> findNeedsReview(Pageable pageable);

    List<RecipeIngredient> findByNeedsReviewTrueAndParsedName(String parsedName);

    @Query("select r.id from Recipe r order by r.createdAt")
    List<UUID> findAllRecipeIds();
}