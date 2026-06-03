package com.backend.backend_pfe.DTO.response;

import lombok.*;

import java.util.List;

/**
 * DTO pour le dashboard Resource Manager — données agrégées.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RmDashboardDTO {

    // KPIs
    private int totalCollaborateurs;
    private int collaborateursActifs;  // ceux avec au moins 1 affectation active
    private double tauxStaffingGlobal; // moyenne des taux d'utilisation des actifs
    private int conflitsDetectes;      // anomalies OUVERTE
    private int ressourcesSurchargees; // collabs > 100%
    private int ressourcesSousUtilisees; // collabs < 80% (avec au moins 1 affectation)

    // Répartition projets
    private int projetsEnCours;
    private int projetsPlanifies;
    private int projetsTermines;

    // Anomalies actives (top 5)
    private List<AnomalieResumeDTO> anomaliesActives;

    // Données mensuelles pour les graphiques (6 derniers mois)
    private List<MoisStaffingDTO> staffingMensuel;
    private List<MoisAnomaliesDTO> anomaliesMensuelles;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MoisStaffingDTO {
        private String mois;
        private double tauxStaffing;
        private double objectif;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MoisAnomaliesDTO {
        private String mois;
        private int surcharge;
        private int sousUtilisation;
        private int conflit;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnomalieResumeDTO {
        private Long id;
        private String type;       // SURCHARGE, CONFLIT_AFFECTATION, DISPONIBILITE_INSUFFISANTE
        private String collaborateur;
        private String projets;
        private double charge;
        private String severite;   // critical, high, medium
    }
}
