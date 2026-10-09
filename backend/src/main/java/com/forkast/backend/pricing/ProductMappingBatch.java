package com.forkast.backend.pricing;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductMappingBatch(
        @NotEmpty @Size(max = 500) List<@Valid @NotNull ProductMappingRequest> products) {
}