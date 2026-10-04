package com.forkast.backend.ingest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.ingredient.IngredientMatcher;
import com.forkast.backend.ingredient.MatchResult;
import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.recipe.RecipeIngredient;
import com.forkast.backend.recipe.RecipeRepository;
import com.forkast.backend.recipe.RecipeStep;

/**
 * Imports ONE recipe in its own transaction.
 *
 * REQUIRES_NEW means each call commits or rolls back on its own, so one bad
 * recipe can't
 * undo the others in a batch. It lives in a separate bean from
 * RecipeIngestService because
 * 
 * @Transactional works through a proxy: a class calling its own method skips
 *                the proxy, and
 *                the annotation would silently do nothing.
 */
@Service
public class RecipeImporter {

    private final RecipeRepository recipeRepository;
    private final IngredientLineParser parser;
    private final IngredientMatcher matcher;
    private final RecipeEnricher enricher;

    public RecipeImporter(RecipeRepository recipeRepository,
            IngredientLineParser parser,
            IngredientMatcher matcher,
            RecipeEnricher enricher) {
        this.recipeRepository = recipeRepository;
        this.parser = parser;
        this.matcher = matcher;
        this.enricher = enricher;
    }

    /**
     * Saves the recipe and returns how many of its lines need review. Throws if the
     * recipe is unusable.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int importRecipe(RecipeIngestRequest request) {
        if (isBlank(request.sourceUrl())) {
            throw new IllegalArgumentException("sourceUrl is required");
        }
        Integer servings = Servings.parse(request.yields());
        if (servings == null) {
            throw new IllegalArgumentException("no serving count in yields: " + request.yields());
        }
        List<String> instructions = nonBlank(request.rawInstructions());
        if (instructions.isEmpty()) {
            throw new IllegalArgumentException("recipe has no instructions");
        }
        List<String> rawIngredients = nonBlank(request.rawIngredients());
        if (rawIngredients.isEmpty()) {
            throw new IllegalArgumentException("recipe has no ingredients");
        }

        Recipe recipe = new Recipe(request.name(), servings);
        recipe.setSource(request.sourceUrl(), request.host());
        recipe.setDescription(request.description());
        recipe.setPrepTimeMinutes(request.prepTimeMinutes());
        recipe.setCookTimeMinutes(request.cookTimeMinutes());
        recipe.setImageUrl(request.imageUrl());
        recipe.setCategory(request.category());
        recipe.setCuisine(request.cuisine());
        recipe.setKeywords(request.keywords() == null ? null : String.join(", ", nonBlank(request.keywords())));

        for (int i = 0; i < instructions.size(); i++) {
            recipe.addStep(new RecipeStep(i + 1, instructions.get(i)));
        }

        int needsReview = 0;
        for (String raw : rawIngredients) {
            ParsedLine parsed = parser.parse(raw);
            if (parsed.skip()) {
                continue; // section headers like "For the sauce:"
            }
            RecipeIngredient line = toRecipeIngredient(raw, parsed);

            MatchResult match = matcher.match(parsed.name());
            if (match.matched()) {
                line.matchIngredient(match.ingredient(), match.score());
            } else {
                line.flagForReview(match.score());
                needsReview++;
            }
            recipe.addIngredient(line);
        }

        enricher.enrich(recipe);
        recipeRepository.save(recipe);
        return needsReview;
    }

    // ---------- helpers ----------

    private static RecipeIngredient toRecipeIngredient(String raw, ParsedLine parsed) {
        RecipeIngredient line = new RecipeIngredient(raw, null, Unit.TO_TASTE.name().toLowerCase(Locale.ROOT));
        applyParse(line, parsed);
        return line;
    }

    /**
     * Copies a parse result onto a saved line. Shared with RecipeReprocessor so a
     * re-parse
     * stores lines exactly the way the first import did.
     */
    static void applyParse(RecipeIngredient line, ParsedLine parsed) {
        // A line with no amount ("cooking spray") is stored like "to taste": no weight,
        // no cost.
        Unit unit = parsed.unit() != null ? parsed.unit() : Unit.TO_TASTE;
        BigDecimal amount = parsed.amount();
        if (amount != null) {
            amount = amount.signum() > 0 ? amount.setScale(3, RoundingMode.HALF_UP) : null;
        }

        line.setAmount(amount);
        line.setUnit(unit.name().toLowerCase(Locale.ROOT));
        line.setParsedName(truncate(parsed.name(), 255));
        line.setPrepNote(truncate(parsed.prepNote(), 255));
        line.setOptional(parsed.optional());
    }

    private static List<String> nonBlank(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}