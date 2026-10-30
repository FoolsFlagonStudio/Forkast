package com.forkast.backend.mealplan.grocery;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forkast.backend.mealplan.GroceryListItem;

public interface GroceryListItemRepository extends JpaRepository<GroceryListItem, UUID> {

    /**
     * A line only the plan's owner can change: anyone else's id is the same as a
     * missing one.
     */
    Optional<GroceryListItem> findByIdAndMealPlanUserId(UUID id, UUID userId);
}