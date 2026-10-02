package com.forkast.backend.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.forkast.backend.user.User;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        boolean verified,
        boolean premium,
        boolean onboardingCompleted,
        Instant createdAt) {

    public static UserResponse from(User user, boolean onboardingCompleted) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.isVerified(),
                user.isPremium(),
                onboardingCompleted,
                user.getCreatedAt());
    }
}