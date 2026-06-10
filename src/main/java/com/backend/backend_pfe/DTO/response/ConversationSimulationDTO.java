package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.ConversationSimulationStatus;
import com.backend.backend_pfe.enums.ResultatSimulationWhatIf;
import com.backend.backend_pfe.enums.StatutSimulationWhatIf;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSimulationDTO {

    private Long id;
    private Long simulationId;
    private ConversationSimulationStatus status;
    private LocalDateTime dateCreation;
    private String createdByNomComplet;

    private String collaborateurSource;
    private String collaborateurCible;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private List<String> projetsConflit;
    private ResultatSimulationWhatIf resultat;
    private StatutSimulationWhatIf statutSimulation;

    private Double tauxSourceAvant;
    private Double tauxSourceApres;
    private Double tauxCibleAvant;
    private Double tauxCibleApres;

    private List<ParticipantDTO> participants;
    private List<SimulationDecisionDTO> decisions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParticipantDTO {
        private Long userId;
        private String nomComplet;
        private String role;
        private Long projetId;
        private String projetNom;
        private boolean chefProjetConcerne;
    }
}
