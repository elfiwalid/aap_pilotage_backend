package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.LoginRequestDTO;
import com.backend.backend_pfe.DTO.response.LoginResponseDTO;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.exception.AuthenticationException;
import com.backend.backend_pfe.security.JwtService;
import com.backend.backend_pfe.security.UserDetailsAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Concrete implementation of {@link AuthService}.
 *
 * SOLID — Single Responsibility Principle (SRP):
 *   Handles authentication logic only: look up the user, verify the
 *   password, and delegate token generation to JwtService.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 *   Depends on abstractions (UserRepository, PasswordEncoder, JwtService)
 *   instead of concrete classes. All injected via constructor.
 *
 * Clean Code — Constructor Injection:
 *   Uses @RequiredArgsConstructor (Lombok) for immutable, testable injection.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        // 1. Find user by email
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new AuthenticationException(
                        "Adresse email introuvable dans le système."));

        // 2. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Mot de passe incorrect.");
        }

        // 3. Generate JWT
        UserDetailsAdapter userDetails = new UserDetailsAdapter(user);
        String token = jwtService.generateToken(userDetails);

        // 4. Build response (password is never returned)
        return LoginResponseDTO.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .build();
    }
}
