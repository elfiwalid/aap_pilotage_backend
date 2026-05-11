package com.backend.backend_pfe.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Interface defining JWT operations.
 * 
 * SOLID — Interface Segregation Principle (ISP):
 *   Only JWT-related methods are exposed here, keeping the contract focused.
 * 
 * SOLID — Dependency Inversion Principle (DIP):
 *   Consumers depend on this abstraction, not on the concrete implementation.
 */
public interface JwtService {

    /**
     * Generate a JWT token for the given user.
     */
    String generateToken(UserDetails userDetails);

    /**
     * Extract the email (subject) from a JWT token.
     */
    String extractEmail(String token);

    /**
     * Check whether a JWT token is valid for the given user.
     */
    boolean isTokenValid(String token, UserDetails userDetails);
}
