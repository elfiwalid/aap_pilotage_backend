package com.backend.backend_pfe.DTO.response;

import lombok.*;

/**
 * DTO returned after a successful login.
 *
 * SOLID — Single Responsibility Principle (SRP):
 *   Contains only the data the frontend needs after authentication.
 *   The password hash is never exposed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String token;
    private String email;
    private String role;
    private String nom;
    private String prenom;
}
