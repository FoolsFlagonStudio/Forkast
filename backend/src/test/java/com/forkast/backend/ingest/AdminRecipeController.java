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

    public AdminRecipeController(RecipeIngestService ingestService) {
        this.ingestService = ingestService;
    }

    /**
     * Always 200 for a well-formed batch; per-recipe problems are listed in
     * "failed".
     */
    @PostMapping("/ingest")
    public RecipeIngestResult ingest(@Valid @RequestBody RecipeIngestBatch batch) {
        return ingestService.ingest(batch.recipes());
    }
}