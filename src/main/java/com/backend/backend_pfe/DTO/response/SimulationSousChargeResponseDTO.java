package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.ResultatSimulationWhatIf;
import com.backend.backend_pfe.enums.TypeSimulationWhatIf;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SimulationSousChargeResponseDTO {

    private Long simulationId;

    private TypeSimulationWhatIf typeSimulation;

    private ResultatSimulationWhatIf resultat;

    private String commentaire;

    private String collaborateurCible;

    private Double joursCibleAvant;
    private Double joursCibleApres;

    private Double tauxCibleAvant;
    private Double tauxCibleApres;

    private String etatCibleAvant;
    private String etatCibleApres;

    private Boolean sousChargeReduite;
    private Boolean nouvelleSurcharge;
    private Boolean nouveauConflit;
}