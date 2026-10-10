package com.forkast.backend.mealplan;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MealPlanRepository extends JpaRepository<MealPlan, UUID> {

        Optional<MealPlan> findByUserIdAndWeekStartDate(UUID userId, LocalDate weekStartDate);

        boolean existsByUserIdAndWeekStartDate(UUID userId, LocalDate weekStartDate);

        List<MealPlan> findByUserIdOrderByWeekStartDateDesc(UUID userId);

        /** One user's plans, a page at a time (sorted by the Pageable). */
        Page<MealPlan> findByUserId(UUID userId, Pageable pageable);

        /**
         * A plan only its owner can see: someone else's id is the same as a missing
         * one.
         */
        Optional<MealPlan> findByIdAndUserId(UUID id, UUID userId);

        /**
         * The plan whose 7 days include `today`: it started on or before today and at
         * most 6 days
         * earlier. Newest first, in case an odd start-day change left two overlapping.
         */
        Optional<MealPlan> findFirstByUserIdAndWeekStartDateLessThanEqualAndWeekStartDateGreaterThanEqualOrderByWeekStartDateDesc(
                        UUID userId, LocalDate today, LocalDate earliestStart);

        /**
         * Plans that started on or before `cutoff` and haven't been written to
         * recipe_history yet.
         */
        @Query("select p.id from MealPlan p where p.historyRecordedAt is null and p.weekStartDate <= :cutoff")
        List<UUID> findIdsWithoutHistoryStartingBy(@Param("cutoff") LocalDate cutoff);

        /**
         * The latest cook date of each recipe in the user's plans that start before
         * `before`.
         */
        @Query("""
                        select new com.forkast.backend.mealplan.LastServed(s.recipe.id, max(s.startDate))
                        from PlanSelection s
                        where s.mealPlan.user.id = :userId
                          and s.startDate < :before
                        group by s.recipe.id
                        """)
        List<LastServed> findLastPlannedDates(@Param("userId") UUID userId, @Param("before") LocalDate before);
}