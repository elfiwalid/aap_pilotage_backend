package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.ResultatSimulationWhatIf;
import com.backend.backend_pfe.enums.TypeSimulationWhatIf;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SimulationRemplacementResponseDTO {

    private Long simulationId;

    private TypeSimulationWhatIf typeSimulation;

    private ResultatSimulationWhatIf resultat;

    private String commentaire;

    private String collaborateurSource;
    private Double joursSourceAvant;
    private Double joursSourceApres;
    private Double tauxSourceAvant;
    private Double tauxSourceApres;
    private String etatSourceApres;

    private String collaborateurCible;
    private Double joursCibleAvant;
    private Double joursCibleApres;
    private Double tauxCibleAvant;
    private Double tauxCibleApres;
    private String etatCibleApres;

    private Boolean conflitCorrige;
    private Boolean nouvelleSurcharge;
    private Boolean nouveauConflit;
    private Boolean sousChargeReduite;
}
