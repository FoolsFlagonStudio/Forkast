package com.forkast.backend.pricing;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientAlias;
import com.forkast.backend.ingredient.IngredientAliasRepository;
import com.forkast.backend.ingredient.IngredientRepository;

/** Finds a catalog ingredient by exact name or alias, ignoring case. No fuzzy matching. */
@Component
public class IngredientLookup {

    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;

    public IngredientLookup(IngredientRepository ingredientRepository, IngredientAliasRepository aliasRepository) {
        this.ingredientRepository = ingredientRepository;
        this.aliasRepository = aliasRepository;
    }

    public Optional<Ingredient> find(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        return ingredientRepository.findByName(key)
                .or(() -> aliasRepository.findByAlias(key).map(IngredientAlias::getIngredient));
    }
}