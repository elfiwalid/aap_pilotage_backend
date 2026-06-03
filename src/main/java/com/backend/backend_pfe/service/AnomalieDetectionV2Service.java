package com.backend.backend_pfe.service;

import com.backend.backend_pfe.Entity.AnomalieV2;
import com.backend.backend_pfe.enums.StatutAnomalieV2;
import com.backend.backend_pfe.enums.TypeAnomalieV2;

import java.util.List;

/**
 * Service de détection des anomalies de staffing V2.
 * Basé sur les jours ouvrables validés et les lignes V2 importées.
 */
public interface AnomalieDetectionV2Service {

    /** Lance la détection complète pour une période (mois/année). */
    List<AnomalieV2> detecterAnomalies(int annee, int mois, String pays);

    /** Liste des anomalies pour une période. */
    List<AnomalieV2> getAnomalies(int annee, int mois);

    /** Liste filtrée par type. */
    List<AnomalieV2> getAnomaliesByType(int annee, int mois, TypeAnomalieV2 type);

    /** Détail d'une anomalie. */
    AnomalieV2 getAnomalie(Long id);

    /** Changer le statut d'une anomalie. */
    void changerStatut(Long id, StatutAnomalieV2 statut);

    /** Taux de charge mensuel d'un collaborateur. */
    double getTauxCharge(Long collaborateurId, int annee, int mois);

    /** Liste des anomalies filtrées par chef de projet (seulement ses collaborateurs). */
    List<AnomalieV2> getAnomaliesParChef(int annee, int mois, Long chefProjetId);

    /** Retourne les périodes (année/mois) pour lesquelles il existe des anomalies. */
    List<int[]> getPeriodesDisponibles();

    /** Retourne les périodes pour un chef de projet spécifique. */
    List<int[]> getPeriodesDisponiblesParChef(Long chefProjetId);

    /** Retourne les affectations détaillées d'un collaborateur pour un mois donné. */
    List<java.util.Map<String, Object>> getAffectationsDetail(Long collaborateurId, int annee, int mois);
}
