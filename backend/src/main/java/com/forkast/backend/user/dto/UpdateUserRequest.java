package com.forkast.backend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String firstName,
        @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String lastName) {
}