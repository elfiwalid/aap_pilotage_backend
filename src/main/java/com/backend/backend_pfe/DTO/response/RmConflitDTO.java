package com.backend.backend_pfe.DTO.response;

import lombok.*;

import java.util.List;

/**
 * DTO pour la vue Conflits du Resource Manager.
 * Représente une anomalie enrichie avec les projets impliqués et les alternatives.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RmConflitDTO {

    private Long id;
    private String collaborateur;
    private String collaborateurEmail;
    private String role;
    private String chefProjet;
    private double tauxCharge;
    private String type;       // "surcharge" ou "sous-utilisation"
    private String severite;   // "critical", "high", "medium", "low"
    private String message;
    private String periode;
    private List<ProjetImplique> projets;
    private List<AlternativeDTO> alternatives;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjetImplique {
        private Long projetId;
        private String nom;
        private String chefProjet;
        private double charge;
        private String couleur;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlternativeDTO {
        private Long collaborateurId;
        private String nom;
        private String prenom;
        private String poste;
        private double disponibilite; // 100 - tauxUtilisation
    }
}
