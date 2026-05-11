package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.LoginRequestDTO;
import com.backend.backend_pfe.DTO.response.LoginResponseDTO;

/**
 * Authentication service contract.
 *
 * SOLID — Interface Segregation Principle (ISP):
 *   Only authentication-related operations are defined here.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 *   The controller depends on this abstraction, not on the implementation.
 */
public interface AuthService {

    /**
     * Authenticate a user with email/password and return a JWT.
     *
     * @param request the login credentials
     * @return a DTO containing the JWT and user profile
     */
    LoginResponseDTO login(LoginRequestDTO request);
}
