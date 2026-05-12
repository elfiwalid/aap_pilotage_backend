package com.backend.backend_pfe.exception;

/**
 * Custom exception for resource not found scenarios.
 *
 * Clean Code — Meaningful Exception:
 *   Provides a domain-specific exception when a requested resource
 *   cannot be found (e.g., chef de projet introuvable via JWT).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
