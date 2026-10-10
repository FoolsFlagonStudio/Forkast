package com.forkast.backend.mealplan.grocery;

/** Either field may be left out to leave it as it is. */
public record UpdateGroceryItemRequest(Boolean purchased, Boolean needToBuy) {
}