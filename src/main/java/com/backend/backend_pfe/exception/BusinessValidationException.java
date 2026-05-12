package com.backend.backend_pfe.exception;

/**
 * Custom exception for business validation failures.
 *
 * Clean Code — Meaningful Exception:
 *   Provides a domain-specific exception for business rule violations
 *   (e.g., dateFin ≤ dateDebut) instead of relying on generic RuntimeException.
 */
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(String message) {
        super(message);
    }
}
