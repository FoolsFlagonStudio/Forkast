package com.forkast.backend.ingredient;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    Optional<Ingredient> findByFdcId(Long fdcId);

    Optional<Ingredient> findByNameIgnoreCase(String name);
}