package com.forkast.backend.ingredient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientPriceRepository extends JpaRepository<IngredientPrice, UUID> {

    Optional<IngredientPrice> findFirstByIngredientIdOrderByRecordedAtDesc(UUID ingredientId);

    List<IngredientPrice> findByIngredientIdOrderByRecordedAtDesc(UUID ingredientId);
}