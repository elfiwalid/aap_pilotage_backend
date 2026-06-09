package com.backend.backend_pfe.DTO.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationConflitContextDTO {

    private Long conflitId;
    private Long collaborateurSourceId;
    private String collaborateurSourceNomComplet;
    private String matricule;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int annee;
    private int mois;
    private double tauxCharge;
    private int joursEnConflit;
    private String description;
    private List<ProjetConflitDTO> projetsConflit;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjetConflitDTO {
        private Long projetId;
        private String projetNom;
        private String chefProjetNomComplet;
        private LocalDate dateDebut;
        private LocalDate dateFin;
        private Double tauxAffectation;
        private int joursOuvrables;
    }
}
