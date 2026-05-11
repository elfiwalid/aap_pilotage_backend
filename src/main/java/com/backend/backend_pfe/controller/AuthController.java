package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.request.LoginRequestDTO;
import com.backend.backend_pfe.DTO.response.LoginResponseDTO;
import com.backend.backend_pfe.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 *
 * SOLID — Single Responsibility Principle (SRP):
 *   This controller only handles HTTP concerns (request mapping,
 *   validation, response formatting). All business logic is
 *   delegated to AuthService.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 *   Depends on the AuthService abstraction, not on AuthServiceImpl.
 *
 * Clean Code — Thin Controller:
 *   The controller is intentionally thin; it validates input
 *   and delegates to the service layer.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticate a user and return a JWT token.
     *
     * @param request LoginRequestDTO with email and password
     * @return LoginResponseDTO containing the JWT and user info
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
