package com.forkast.backend.auth.dto;

import java.time.Instant;

import com.forkast.backend.auth.TokenService;
import com.forkast.backend.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        UserResponse user) {

    public static AuthResponse of(TokenService.IssuedTokens tokens, UserResponse user) {
        return new AuthResponse(tokens.accessToken(), tokens.accessTokenExpiresAt(), tokens.refreshToken(), user);
    }
}