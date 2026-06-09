package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.SimulationDecisionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationDecisionDTO {

    private Long id;
    private Long chefProjetId;
    private String chefProjetNomComplet;
    private Long projetId;
    private String projetNom;
    private SimulationDecisionStatus status;
    private String commentaire;
    private LocalDateTime dateDecision;
}
