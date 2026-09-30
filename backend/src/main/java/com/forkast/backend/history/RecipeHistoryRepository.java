package com.forkast.backend.history;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecipeHistoryRepository extends JpaRepository<RecipeHistory, UUID> {

    List<RecipeHistory> findTop20ByUserIdOrderByServedAtDesc(UUID userId);

    List<RecipeHistory> findByUserIdAndServedAtAfter(UUID userId, Instant cutoff);

    @Query("""
            select distinct h.recipe.id
            from RecipeHistory h
            where h.user.id = :userId
              and h.servedAt > :cutoff
            """)
    Set<UUID> findRecentRecipeIds(UUID userId, Instant cutoff);
}