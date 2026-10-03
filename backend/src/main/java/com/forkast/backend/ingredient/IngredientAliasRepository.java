package com.forkast.backend.ingredient;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientAliasRepository extends JpaRepository<IngredientAlias, UUID> {

    Optional<IngredientAlias> findByAlias(String alias);
}