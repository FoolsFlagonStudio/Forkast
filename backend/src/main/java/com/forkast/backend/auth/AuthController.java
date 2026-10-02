package com.forkast.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forkast.backend.auth.dto.AuthResponse;
import com.forkast.backend.auth.dto.ConfirmEmailChangeRequest;
import com.forkast.backend.auth.dto.EmailChangeRequest;
import com.forkast.backend.auth.dto.EmailRequest;
import com.forkast.backend.auth.dto.LoginRequest;
import com.forkast.backend.auth.dto.RefreshRequest;
import com.forkast.backend.auth.dto.ResetPasswordRequest;
import com.forkast.backend.auth.dto.SignupRequest;
import com.forkast.backend.auth.dto.UpdatePasswordRequest;
import com.forkast.backend.auth.dto.VerifyEmailRequest;
import com.forkast.backend.common.MessageResponse;
import com.forkast.backend.user.dto.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return new MessageResponse("Account created. Check your email for a 6-digit verification code.");
    }

    @PostMapping("/verify-email")
    public AuthResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse resendVerification(@Valid @RequestBody EmailRequest request) {
        authService.resendVerification(request);
        return new MessageResponse("If that account needs verifying, we've sent a new code.");
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse forgotPassword(@Valid @RequestBody EmailRequest request) {
        authService.forgotPassword(request);
        return new MessageResponse("If an account exists for that email, we've sent a reset code.");
    }

    @PostMapping("/reset-password")
    public AuthResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PatchMapping("/password")
    public AuthResponse updatePassword(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdatePasswordRequest request) {
        return authService.updatePassword(currentUser.id(), request);
    }

    @PostMapping("/email-change")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse requestEmailChange(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody EmailChangeRequest request) {
        authService.requestEmailChange(currentUser.id(), request);
        return new MessageResponse("We've sent a 6-digit code to your new email address.");
    }

    @PostMapping("/email-change/confirm")
    public UserResponse confirmEmailChange(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ConfirmEmailChangeRequest request) {
        return authService.confirmEmailChange(currentUser.id(), request);
    }
}