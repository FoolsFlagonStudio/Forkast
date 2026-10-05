package com.forkast.backend.recipe;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.forkast.backend.auth.AuthenticatedUser;
import com.forkast.backend.common.PageResponse;
import com.forkast.backend.recipe.search.RecipeSearchRequest;
import com.forkast.backend.recipe.search.RecipeSummaryResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Recipe routes for the app. Every route requires a Bearer token
 * (SecurityConfig's default).
 */
@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeQueryService queryService;

    public RecipeController(RecipeQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * @ModelAttribute builds the record from query parameters instead of a JSON
     *                 body.
     */
    @GetMapping
    public PageResponse<RecipeSummaryResponse> search(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @ModelAttribute RecipeSearchRequest request) {
        return queryService.search(currentUser.id(), request);
    }

    /** ?servings= is optional; without it the user's household size is used. */
    @GetMapping("/{id}")
    public RecipeDetailResponse detail(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @RequestParam(required = false) @Min(1) @Max(50) Integer servings) {
        return queryService.detail(currentUser.id(), id, servings);
    }
}