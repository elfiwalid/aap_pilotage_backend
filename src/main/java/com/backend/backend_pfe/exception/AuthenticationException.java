package com.backend.backend_pfe.exception;

/**
 * Custom exception for authentication failures.
 *
 * Clean Code — Meaningful Exception:
 *   Provides a domain-specific exception instead of relying
 *   on generic RuntimeException.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
