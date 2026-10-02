package com.forkast.backend.auth;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.forkast.backend.common.exception.ApiException;
import com.forkast.backend.email.EmailService;
import com.forkast.backend.user.User;
import com.forkast.backend.user.UserRepository;
import com.forkast.backend.user.UserService;
import com.forkast.backend.user.dto.UserResponse;

@Service
public class AuthService {

    private static final String INVALID_CODE = "That code is invalid or has expired. Request a new one.";

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthCodeService authCodeService;
    private final TokenService tokenService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
            UserService userService,
            PasswordEncoder passwordEncoder,
            AuthCodeService authCodeService,
            TokenService tokenService,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authCodeService = authCodeService;
        this.tokenService = tokenService;
        this.emailService = emailService;
    }

    @Transactional
    public void signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("An account with that email already exists.");
        }

        User user = userRepository.save(new User(
                request.firstName(),
                request.lastName(),
                email,
                passwordEncoder.encode(request.password())));

        String code = authCodeService.issue(user, AuthCodeType.EMAIL_VERIFY, null);
        emailService.sendVerificationCode(user.getEmail(), user.getFirstName(), code);
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> ApiException.badRequest(INVALID_CODE));

        if (user.isVerified()) {
            throw ApiException.badRequest("This account is already verified. Please log in.");
        }

        authCodeService.consume(user, AuthCodeType.EMAIL_VERIFY, request.code());

        user.markVerified();
        user.recordLogin();
        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Incorrect email or password."));

        if (!user.isVerified()) {
            throw ApiException.forbidden("Please verify your email before logging in.");
        }

        user.recordLogin();
        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        User user = tokenService.consumeRefreshToken(request.refreshToken());
        return issueAuthResponse(user);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        tokenService.revokeRefreshToken(request.refreshToken());
    }

    @Transactional
    public void resendVerification(EmailRequest request) {
        userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(user -> !user.isVerified())
                .ifPresent(user -> {
                    String code = authCodeService.issue(user, AuthCodeType.EMAIL_VERIFY, null);
                    emailService.sendVerificationCode(user.getEmail(), user.getFirstName(), code);
                });
    }

    @Transactional
    public void forgotPassword(EmailRequest request) {
        userRepository.findByEmail(normalizeEmail(request.email()))
                .ifPresent(user -> {
                    String code = authCodeService.issue(user, AuthCodeType.PASSWORD_RESET, null);
                    emailService.sendPasswordResetCode(user.getEmail(), user.getFirstName(), code);
                });
    }

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> ApiException.badRequest(INVALID_CODE));

        authCodeService.consume(user, AuthCodeType.PASSWORD_RESET, request.code());

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        user.markVerified(); // receiving the code proves they own the email
        user.recordLogin();
        tokenService.revokeAllSessions(user);
        emailService.sendPasswordChangedNotice(user.getEmail(), user.getFirstName());

        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse updatePassword(UUID userId, UpdatePasswordRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Your current password is incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Your new password must be different from your current one.");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        tokenService.revokeAllSessions(user);
        emailService.sendPasswordChangedNotice(user.getEmail(), user.getFirstName());

        return issueAuthResponse(user);
    }

    @Transactional
    public void requestEmailChange(UUID userId, EmailChangeRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Your current password is incorrect.");
        }

        String newEmail = normalizeEmail(request.newEmail());
        if (newEmail.equals(user.getEmail())) {
            throw ApiException.badRequest("That's already your email address.");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw ApiException.conflict("An account with that email already exists.");
        }

        String code = authCodeService.issue(user, AuthCodeType.EMAIL_CHANGE, newEmail);
        emailService.sendEmailChangeCode(newEmail, user.getFirstName(), code);
    }

    @Transactional
    public UserResponse confirmEmailChange(UUID userId, ConfirmEmailChangeRequest request) {
        User user = findUser(userId);
        AuthCode code = authCodeService.consume(user, AuthCodeType.EMAIL_CHANGE, request.code());

        String newEmail = code.getNewEmail();
        if (userRepository.existsByEmail(newEmail)) {
            throw ApiException.conflict("An account with that email already exists.");
        }

        String oldEmail = user.getEmail();
        user.changeEmail(newEmail);
        emailService.sendEmailChangedNotice(oldEmail, user.getFirstName(), newEmail);

        return userService.toResponse(user);
    }

    // ---------- helpers ----------

    private AuthResponse issueAuthResponse(User user) {
        TokenService.IssuedTokens tokens = tokenService.issueTokens(user);
        return AuthResponse.of(tokens, userService.toResponse(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found."));
    }
}