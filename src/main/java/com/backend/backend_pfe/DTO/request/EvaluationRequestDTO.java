package com.backend.backend_pfe.DTO.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for submitting a performance evaluation.
 *
 * SOLID — SRP: contains only the data needed to create/update an evaluation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationRequestDTO {

    @NotNull(message = "L'identifiant du collaborateur est obligatoire")
    private Long collaborateurId;

    @NotNull(message = "Le mois est obligatoire")
    @Min(value = 1, message = "Le mois doit être entre 1 et 12")
    @Max(value = 12, message = "Le mois doit être entre 1 et 12")
    private Integer mois;

    @NotNull(message = "L'année est obligatoire")
    @Min(value = 2020, message = "L'année doit être valide")
    private Integer annee;

    @NotNull(message = "La note qualité du travail est obligatoire")
    @DecimalMin(value = "0.0") @DecimalMax(value = "5.0")
    private Double qualiteTravail;

    @NotNull(message = "La note respect des délais est obligatoire")
    @DecimalMin(value = "0.0") @DecimalMax(value = "5.0")
    private Double respectDelais;

    @NotNull(message = "La note travail en équipe est obligatoire")
    @DecimalMin(value = "0.0") @DecimalMax(value = "5.0")
    private Double travailEquipe;

    @NotNull(message = "La note communication est obligatoire")
    @DecimalMin(value = "0.0") @DecimalMax(value = "5.0")
    private Double communication;

    private String commentaire;
}
