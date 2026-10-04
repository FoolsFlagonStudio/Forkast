package com.forkast.backend.ingest;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecipeIngestBatch(
        @NotEmpty @Size(max = 25) List<@NotNull RecipeIngestRequest> recipes) {
}