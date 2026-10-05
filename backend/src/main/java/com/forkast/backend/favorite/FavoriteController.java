package com.forkast.backend.favorite;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forkast.backend.auth.AuthenticatedUser;
import com.forkast.backend.common.PageResponse;
import com.forkast.backend.recipe.RecipeQueryService;
import com.forkast.backend.recipe.search.RecipeSummaryResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final RecipeQueryService recipeQueryService;

    public FavoriteController(FavoriteService favoriteService, RecipeQueryService recipeQueryService) {
        this.favoriteService = favoriteService;
        this.recipeQueryService = recipeQueryService;
    }

    /**
     * PUT rather than POST: it sets a state, so sending it twice is the same as
     * once.
     */
    @PutMapping("/api/recipes/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id) {
        favoriteService.add(currentUser.id(), id);
    }

    @DeleteMapping("/api/recipes/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id) {
        favoriteService.remove(currentUser.id(), id);
    }

    @GetMapping("/api/users/me/favorites")
    public PageResponse<RecipeSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return recipeQueryService.favorites(currentUser.id(), page, size);
    }
}