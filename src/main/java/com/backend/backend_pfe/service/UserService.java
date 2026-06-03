package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.UpdateProfileRequestDTO;
import com.backend.backend_pfe.DTO.response.UserProfileDTO;
import org.springframework.security.core.Authentication;

/**
 * Service interface for user profile operations.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 *   Controllers depend on this abstraction, not on the implementation.
 */
public interface UserService {

    /**
     * Get the profile of the currently authenticated user.
     */
    UserProfileDTO getMyProfile(Authentication authentication);

    /**
     * Update the profile of the currently authenticated user.
     */
    UserProfileDTO updateMyProfile(Authentication authentication, UpdateProfileRequestDTO request);

    /**
     * Get all users in the system.
     */
    java.util.List<UserProfileDTO> getAllUsers();
}
