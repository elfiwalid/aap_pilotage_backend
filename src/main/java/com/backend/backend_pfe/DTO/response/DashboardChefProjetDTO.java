package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.StatutProjet;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de tableau de bord pour le Chef de Projet.
 * Agrège les KPIs en un seul appel API.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardChefProjetDTO {

    // ─── KPIs principaux ───────────────────────────────────────────
    private int totalProjets;
    private int projetsActifs;
    private int projetsTermines;
    private int projetsEnAttente;

    private int totalCollaborateurs;  // collaborateurs distincts sur tous les projets actifs

    private int totalAnomaliesMoisCourant;
    private int anomaliesCritiques;   // SURCHARGE ou CONFLIT
    private int anomaliesActives;     // statut DETECTEE ou EN_COURS_TRAITEMENT

    // ─── Données pour les graphiques ──────────────────────────────
    /** Évolution mensuelle du nombre de collaborateurs actifs (12 derniers mois) */
    private List<MoisCollabDTO> evolutionCollaborateurs;

    /** Performance par projet (avancement en % basé sur la durée écoulée) */
    private List<ProjetPerfDTO> performanceProjets;

    /** Anomalies des 6 derniers mois pour le graphique de tendance */
    private List<MoisAnomalieDTO> tendanceAnomalies;

    // ─── Données pour les listes ──────────────────────────────────
    /** Les 5 anomalies les plus récentes pour le chef connecté */
    private List<AnomalieResumeeDTO> anomaliesRecentes;

    /** Les projets du chef (tous) */
    private List<ProjetResumeeDTO> projetsRecents;

    // ─── Nested DTOs ──────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MoisCollabDTO {
        private String mois;       // "Jan 2026"
        private int annee;
        private int moisNum;
        private int collaborateurs;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProjetPerfDTO {
        private Long id;
        private String nom;
        private int avancementPct;     // % de la durée écoulée (0-100)
        private int collaborateurs;    // nb de collaborateurs assignés
        private StatutProjet statut;
        private LocalDate dateDebut;
        private LocalDate dateFin;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MoisAnomalieDTO {
        private String mois;       // "Jan"
        private int annee;
        private int moisNum;
        private int total;
        private int surcharges;
        private int conflits;
        private int sousCharges;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AnomalieResumeeDTO {
        private Long id;
        private String collaborateurNom;
        private String typeAnomalie;
        private String statut;
        private double tauxCharge;
        private String projetsConcernes;
        private int annee;
        private int mois;
        private String dateDetection;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProjetResumeeDTO {
        private Long id;
        private String nom;
        private StatutProjet statut;
        private LocalDate dateDebut;
        private LocalDate dateFin;
        private int avancementPct;
        private int collaborateurs;
    }
}
