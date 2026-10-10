package com.forkast.backend.mealplan.grocery;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forkast.backend.auth.AuthenticatedUser;

@RestController
public class GroceryListController {

    private final GroceryListService groceryListService;

    public GroceryListController(GroceryListService groceryListService) {
        this.groceryListService = groceryListService;
    }

    @GetMapping("/api/meal-plans/{planId}/grocery-list")
    public GroceryListResponse list(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID planId) {
        return groceryListService.list(currentUser.id(), planId);
    }

    /** {purchased?, needToBuy?}; needToBuy only applies to pantry staples. */
    @PatchMapping("/api/grocery-list-items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id,
            @RequestBody UpdateGroceryItemRequest request) {
        groceryListService.update(currentUser.id(), id, request);
    }
}