package com.forkast.backend.user;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.common.exception.ApiException;
import com.forkast.backend.user.dto.DeleteAccountRequest;
import com.forkast.backend.user.dto.UpdateUserRequest;
import com.forkast.backend.user.dto.UserResponse;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
            UserPreferencesRepository userPreferencesRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found."));
        return toResponse(user);
    }

    public UserResponse toResponse(User user) {
        boolean onboardingCompleted = userPreferencesRepository.existsByUserId(user.getId());
        return UserResponse.from(user, onboardingCompleted);
    }

    @Transactional
    public void deleteAccount(UUID userId, DeleteAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.badRequest("Your password is incorrect.");
        }

        userRepository.delete(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateUserRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found."));

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }

        return toResponse(user);
    }

}