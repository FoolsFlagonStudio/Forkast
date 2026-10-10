package com.forkast.backend.history;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /** The most recent time the user had each recipe. */
  @Query("""
      select new com.forkast.backend.history.LastServedAt(h.recipe.id, max(h.servedAt))
      from RecipeHistory h
      where h.user.id = :userId
      group by h.recipe.id
      """)
  List<LastServedAt> findLastServed(@Param("userId") UUID userId);
}