package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.ResultatSimulationWhatIf;
import com.backend.backend_pfe.enums.TypeSimulationWhatIf;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
public class SimulationRemplacementResponseDTO {

    private Long simulationId;

    private TypeSimulationWhatIf typeSimulation;

    private ResultatSimulationWhatIf resultat;

    private String commentaire;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private List<String> projetsConflit;

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
