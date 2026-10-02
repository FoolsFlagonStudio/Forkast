package com.forkast.backend.recipe;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    List<Recipe> findByNameContainingIgnoreCase(String text);
}