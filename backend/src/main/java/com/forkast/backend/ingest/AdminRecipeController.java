package com.forkast.backend.ingest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/recipes")
public class AdminRecipeController {

    private final RecipeIngestService ingestService;
    private final RecipeRescoreService rescoreService;

    public AdminRecipeController(RecipeIngestService ingestService, RecipeRescoreService rescoreService) {
        this.ingestService = ingestService;
        this.rescoreService = rescoreService;
    }

    /**
     * Always 200 for a well-formed batch; per-recipe problems are listed in
     * "failed".
     */
    @PostMapping("/ingest")
    public RecipeIngestResult ingest(@Valid @RequestBody RecipeIngestBatch batch) {
        return ingestService.ingest(batch.recipes());
    }

    /**
     * Recomputes every recipe's breakfast, lunch, dinner and side scores from its
     * name, category
     * and keywords. Run after changing SlotClassifier (and once after V11, which
     * adds side scores).
     */
    @PostMapping("/rescore")
    public RecipeRescoreService.Result rescore() {
        return rescoreService.rescoreAll();
    }
}
