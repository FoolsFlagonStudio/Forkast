package com.forkast.backend.ingest;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class AdminReviewController {

    private final IngredientReviewService reviewService;
    private final RecipeReprocessService reprocessService;

    public AdminReviewController(IngredientReviewService reviewService, RecipeReprocessService reprocessService) {
        this.reviewService = reviewService;
        this.reprocessService = reprocessService;
    }

    @GetMapping("/api/admin/recipe-ingredients/needs-review")
    public ReviewPage needsReview(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return reviewService.needsReview(page, size);
    }

    @PatchMapping("/api/admin/recipe-ingredients/{id}")
    public ResolveReviewResponse resolve(@PathVariable UUID id,
            @Valid @RequestBody ResolveReviewRequest request) {
        return reviewService.resolve(id, request);
    }

    /**
     * Re-parses saved lines from their raw text; rematchAll=true also re-matches
     * lines that already matched.
     */
    @PostMapping("/api/admin/recipes/reprocess")
    public ReprocessResult reprocess(@RequestParam(defaultValue = "false") boolean rematchAll) {
        return reprocessService.reprocessAll(rematchAll);
    }
}