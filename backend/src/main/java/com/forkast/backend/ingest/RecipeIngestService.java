package com.forkast.backend.ingest;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.forkast.backend.recipe.RecipeRepository;

/**
 * Runs a batch through RecipeImporter one recipe at a time and reports what
 * happened.
 *
 * Not @Transactional on purpose: each recipe gets its own transaction inside
 * the importer,
 * and a failure is caught here and recorded instead of failing the request.
 */
@Service
public class RecipeIngestService {

    private static final Logger log = LoggerFactory.getLogger(RecipeIngestService.class);

    private final RecipeRepository recipeRepository;
    private final RecipeImporter importer;

    public RecipeIngestService(RecipeRepository recipeRepository, RecipeImporter importer) {
        this.recipeRepository = recipeRepository;
        this.importer = importer;
    }

    public RecipeIngestResult ingest(List<RecipeIngestRequest> recipes) {
        int created = 0;
        int skippedDuplicate = 0;
        int needsReview = 0;
        List<RecipeIngestResult.Failure> failed = new ArrayList<>();

        for (RecipeIngestRequest recipe : recipes) {
            String url = recipe.sourceUrl() == null ? null : recipe.sourceUrl().trim();
            try {
                if (url != null && !url.isEmpty() && recipeRepository.existsBySourceUrl(url)) {
                    skippedDuplicate++;
                    continue;
                }
                needsReview += importer.importRecipe(recipe);
                created++;
            } catch (RuntimeException ex) {
                log.warn("Recipe import failed for {}: {}", url, ex.getMessage());
                failed.add(new RecipeIngestResult.Failure(url, reason(ex)));
            }
        }

        return new RecipeIngestResult(created, skippedDuplicate, failed, needsReview);
    }

    private static String reason(RuntimeException ex) {
        if (ex instanceof IllegalArgumentException) {
            return ex.getMessage(); // our own validation messages are safe to return
        }
        if (ex instanceof DataIntegrityViolationException) {
            return "conflicts with existing data (duplicate sourceUrl or a value too long)";
        }
        return ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }
}