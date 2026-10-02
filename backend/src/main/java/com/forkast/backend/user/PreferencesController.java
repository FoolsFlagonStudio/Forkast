package com.forkast.backend.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forkast.backend.auth.AuthenticatedUser;
import com.forkast.backend.user.dto.PreferencesRequest;
import com.forkast.backend.user.dto.PreferencesResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/me/preferences")
public class PreferencesController {

    private final PreferencesService preferencesService;

    public PreferencesController(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @GetMapping
    public PreferencesResponse get(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return preferencesService.getUserPreferences(currentUser.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PreferencesResponse create(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody PreferencesRequest request) {
        return preferencesService.create(currentUser.id(), request);
    }

    @PutMapping
    public PreferencesResponse replace(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody PreferencesRequest request) {
        return preferencesService.replace(currentUser.id(), request);
    }
}