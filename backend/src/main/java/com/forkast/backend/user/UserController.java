package com.forkast.backend.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forkast.backend.auth.AuthenticatedUser;
import com.forkast.backend.user.dto.DeleteAccountRequest;
import com.forkast.backend.user.dto.UpdateUserRequest;
import com.forkast.backend.user.dto.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.getCurrentUser(currentUser.id());
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateProfile(currentUser.id(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteAccount(currentUser.id(), request);
    }
}