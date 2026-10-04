package com.forkast.backend.ingest;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.common.exception.ApiException;
import com.forkast.backend.ingredient.Ingredient;
import com.forkast.backend.ingredient.IngredientAlias;
import com.forkast.backend.ingredient.IngredientAliasRepository;
import com.forkast.backend.ingredient.IngredientNames;
import com.forkast.backend.ingredient.IngredientRepository;
import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.recipe.RecipeIngredient;

@Service
public class IngredientReviewService {

    private static final BigDecimal MANUAL_MATCH = new BigDecimal("1.000");
    private static final int MAX_PAGE_SIZE = 200;

    private final ReviewLineRepository reviewLineRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;
    private final RecipeEnricher enricher;

    public IngredientReviewService(ReviewLineRepository reviewLineRepository,
            IngredientRepository ingredientRepository,
            IngredientAliasRepository aliasRepository,
            RecipeEnricher enricher) {
        this.reviewLineRepository = reviewLineRepository;
        this.ingredientRepository = ingredientRepository;
        this.aliasRepository = aliasRepository;
        this.enricher = enricher;
    }

    @Transactional(readOnly = true)
    public ReviewPage needsReview(int page, int size) {
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Page<ReviewLineResponse> result = reviewLineRepository
                .findNeedsReview(PageRequest.of(Math.max(page, 0), safeSize));
        return new ReviewPage(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional
    public ResolveReviewResponse resolve(UUID lineId, ResolveReviewRequest request) {
        RecipeIngredient line = reviewLineRepository.findById(lineId)
                .orElseThrow(() -> ApiException.notFound("Ingredient line not found."));
        Ingredient ingredient = ingredientRepository.findById(request.ingredientId())
                .orElseThrow(() -> ApiException.notFound("Ingredient not found."));

        line.matchIngredient(ingredient, MANUAL_MATCH);
        Set<Recipe> changed = new LinkedHashSet<>();
        changed.add(line.getRecipe());

        int alsoMatched = 0;
        if (Boolean.TRUE.equals(request.saveAlias()) && line.getParsedName() != null) {
            saveAlias(ingredient, line.getParsedName());

            // Everything else in the queue with the same text is the same ingredient.
            List<RecipeIngredient> sameName = reviewLineRepository
                    .findByNeedsReviewTrueAndParsedName(line.getParsedName());
            for (RecipeIngredient other : sameName) {
                if (Objects.equals(other.getId(), line.getId())) {
                    continue;
                }
                other.matchIngredient(ingredient, MANUAL_MATCH);
                changed.add(other.getRecipe());
                alsoMatched++;
            }
        }

        // Nutrition and labels depend on every line, so recompute each recipe that
        // changed.
        changed.forEach(enricher::enrich);
        return new ResolveReviewResponse(line.getId(), ingredient.getName(), alsoMatched);
    }

    // ---------- helpers ----------

    /**
     * Saves the parsed name, normalized the way the matcher looks it up, as an
     * alias.
     */
    private void saveAlias(Ingredient ingredient, String parsedName) {
        List<String> candidates = IngredientNames.candidates(parsedName);
        if (candidates.isEmpty()) {
            return;
        }
        String alias = candidates.get(0);
        if (alias.equals(ingredient.getName())) {
            return;
        }

        Optional<IngredientAlias> existing = aliasRepository.findByAlias(alias);
        if (existing.isPresent()) {
            if (!Objects.equals(existing.get().getIngredient().getId(), ingredient.getId())) {
                throw ApiException.conflict("'" + alias + "' is already an alias of another ingredient.");
            }
            return; // already saved for this ingredient
        }
        if (ingredientRepository.findByName(alias).isPresent()) {
            throw ApiException.conflict("'" + alias + "' is already the name of another ingredient.");
        }
        ingredient.addAlias(alias);
    }
}