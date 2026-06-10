package com.backend.backend_pfe.DTO.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceForecastRequestDTO {

    @Min(1)
    @Max(12)
    private int mois;

    @Min(2000)
    private int annee;

    @Min(0)
    @JsonAlias("duree_projet_jours")
    private int dureeProjetJours;

    @Min(0)
    @JsonAlias("nb_collaborateurs_actuels")
    private int nbCollaborateursActuels;

    @Min(0)
    @JsonAlias("charge_moyenne")
    private double chargeMoyenne;

    @Min(0)
    @JsonAlias("charge_max")
    private double chargeMax;

    @Min(0)
    @JsonAlias("nb_conflits")
    private int nbConflits;

    @Min(0)
    @JsonAlias("nb_surcharges")
    private int nbSurcharges;

    @Min(0)
    @JsonAlias("nb_sous_charges")
    private int nbSousCharges;

    @Min(0)
    @JsonAlias("nb_anomalies_total")
    private int nbAnomaliesTotal;

    @Min(0)
    @JsonAlias("nb_collaborateurs_concernes")
    private int nbCollaborateursConcernes;
}
