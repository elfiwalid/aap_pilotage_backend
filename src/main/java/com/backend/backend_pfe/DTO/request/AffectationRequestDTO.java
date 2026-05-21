package com.backend.backend_pfe.DTO.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationRequestDTO {
    
    @NotNull(message = "L'ID du projet est obligatoire")
    private Long projetId;

    @NotNull(message = "L'ID du collaborateur est obligatoire")
    private Long collaborateurId;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    @NotNull(message = "Le taux d'affectation est obligatoire")
    @Positive(message = "Le taux doit être positif")
    private Double tauxAffectation;

    private String roleDansProjet;
}
