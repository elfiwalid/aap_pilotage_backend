package com.backend.backend_pfe.DTO.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO returned when fetching evaluation data.
 *
 * SOLID — SRP: contains all display fields needed by the frontend,
 * including derived fields like moyenneGenerale and user names.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResponseDTO {

    private Long id;

    private Long collaborateurId;
    private String collaborateurNom;
    private String collaborateurPrenom;

    private Long evaluateurId;
    private String evaluateurNom;
    private String evaluateurPrenom;

    private Integer mois;
    private Integer annee;

    private Double qualiteTravail;
    private Double respectDelais;
    private Double travailEquipe;
    private Double communication;
    private Double moyenneGenerale;

    private String commentaire;
    private LocalDateTime dateCreation;
}
