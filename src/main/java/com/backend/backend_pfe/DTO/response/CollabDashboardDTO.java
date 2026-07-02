package com.backend.backend_pfe.DTO.response;

import lombok.*;

import java.util.List;

/**
 * Données agrégées pour le dashboard du collaborateur.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabDashboardDTO {

    /** Nombre de projets actifs assignés */
    private int projetsAssignes;
    /** Taux de charge cumulé (somme des taux d'affectation actifs) */
    private double tauxCharge;
    /** Capacité restante (100 - tauxCharge, min 0) */
    private double capaciteRestante;
    /** Nombre de projets se terminant dans les 30 prochains jours */
    private int projetsBientotTermines;
    /** Avancement moyen des projets actifs */
    private int avancementMoyen;
    private int totalTaches;
    private int tachesTerminees;
    private int tachesEnCours;
    private int tachesBloquees;
    private int tachesEnAttente;
    private double avancementGlobalTaches;

    /** Liste des projets du collaborateur (pour les graphiques et la liste) */
    private List<CollabProjetDTO> projets;

    /** Charge mensuelle prévue par mois (12 mois glissants) */
    private List<ChargeMensuelleDTO> chargeMensuelle;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChargeMensuelleDTO {
        private String mois;       // ex: "Avr"
        private int annee;         // ex: 2026
        private double tauxCharge; // somme des taux d'affectation chevauchant ce mois
        private int nombreProjets; // nombre de projets actifs ce mois
    }
}
