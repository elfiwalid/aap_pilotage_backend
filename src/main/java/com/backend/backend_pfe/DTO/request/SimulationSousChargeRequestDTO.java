package com.backend.backend_pfe.DTO.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SimulationSousChargeRequestDTO {

    @NotNull(message = "L'anomalie est obligatoire")
    private Long anomalieId;

    @NotNull(message = "Le collaborateur cible est obligatoire")
    private Long collaborateurCibleId;

    @NotNull(message = "Le projet est obligatoire")
    private Long projetId;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    private Double tauxAffectation;

    @NotNull(message = "Le Resource Manager est obligatoire")
    private Long resourceManagerId;

    private Integer annee;

    private Integer mois;

    private String pays;
}