package com.backend.backend_pfe.DTO.response;

import lombok.*;

/**
 * DTO returned when fetching the authenticated user's profile.
 *
 * SOLID — SRP: contains only the fields the frontend needs
 * to display and edit a user profile. Password is never exposed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String poste;
    private String photoUrl;
    private String matricule;
    private String role;
    private Double tauxStaffing;
    private Boolean disponible;
}
