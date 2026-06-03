package com.backend.backend_pfe.DTO.response;

import lombok.*;

import java.util.List;

/**
 * DTO pour la vue Resource Manager : un collaborateur avec ses affectations,
 * son taux d'utilisation global, et sa heatmap mensuelle.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RmResourceDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String poste;
    private String matricule;
    private double tauxUtilisation;
    private boolean disponible;
    private List<ProjetAffecteDTO> projets;
    /** 12 valeurs (Jan-Déc) : taux d'utilisation mensuel */
    private List<Double> heatmap;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjetAffecteDTO {
        private Long projetId;
        private String projetNom;
        private double tauxAffectation;
        private String couleur;
    }
}
