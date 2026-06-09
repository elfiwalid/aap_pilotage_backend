package com.backend.backend_pfe.DTO.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimulationDepuisConflitRequestDTO {

    @NotNull
    private Long conflitId;

    @NotNull
    private Long collaborateurCibleId;

    @NotNull
    private Long resourceManagerId;

    private Double tauxAffectation;

    private String pays;
}
