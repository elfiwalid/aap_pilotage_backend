package com.backend.backend_pfe.DTO.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SimulationRemplacementRequestDTO {

    @NotNull
    private Long anomalieId;

    @NotNull
    private Long collaborateurSourceId;

    @NotNull
    private Long collaborateurCibleId;

    @NotNull
    private Long projetId;

    @NotNull
    private LocalDate dateDebut;

    @NotNull
    private LocalDate dateFin;

    private Double tauxAffectation;

    @NotNull
    private Long resourceManagerId;

    private Integer annee;

    private Integer mois;

    private String pays;
}
