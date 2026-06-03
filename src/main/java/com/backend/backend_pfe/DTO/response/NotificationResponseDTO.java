package com.backend.backend_pfe.DTO.response;

import lombok.*;

/**
 * DTO de réponse pour une notification destinée au frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDTO {

    private Long id;
    private String titre;
    private String message;
    private String type;          // ANOMALIE, AFFECTATION, PROJET, SYSTEME
    private String dateCreation;  // ISO-8601
    private boolean lu;           // true si statut = LUE
    private String expediteurNomComplet;
}
