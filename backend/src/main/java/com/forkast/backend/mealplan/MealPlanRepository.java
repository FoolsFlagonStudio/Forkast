package com.forkast.backend.mealplan;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MealPlanRepository extends JpaRepository<MealPlan, UUID> {

    Optional<MealPlan> findByUserIdAndWeekStartDate(UUID userId, LocalDate weekStartDate);

    boolean existsByUserIdAndWeekStartDate(UUID userId, LocalDate weekStartDate);

    List<MealPlan> findByUserIdOrderByWeekStartDateDesc(UUID userId);
}