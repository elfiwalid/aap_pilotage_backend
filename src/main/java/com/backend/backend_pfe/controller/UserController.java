package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.request.UpdateProfileRequestDTO;
import com.backend.backend_pfe.DTO.response.UserProfileDTO;
import com.backend.backend_pfe.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user profile endpoints.
 *
 * SOLID — SRP: handles only HTTP concerns for user profile.
 * SOLID — DIP: depends on the UserService abstraction.
 *
 * Clean Code — Thin Controller: delegates all business logic to the service layer.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/me — Get the authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getMyProfile(authentication));
    }

    /**
     * PUT /api/users/me — Update the authenticated user's profile.
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequestDTO request) {
        return ResponseEntity.ok(userService.updateMyProfile(authentication, request));
    }

    /**
     * GET /api/users — Get all users (for evaluation form, etc.).
     * Accessible to all authenticated users.
     */
    @GetMapping
    public ResponseEntity<List<UserProfileDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
