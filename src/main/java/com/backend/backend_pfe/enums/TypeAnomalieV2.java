package com.backend.backend_pfe.enums;

/**
 * Types d'anomalies de staffing détectées.
 */
public enum TypeAnomalieV2 {
    CONFLIT,          // Chevauchement de dates entre projets
    SURCHARGE,        // Jours demandés > capacité mensuelle
    SOUS_CHARGE,      // Jours demandés < capacité mensuelle
    NON_STAFFE        // Collaborateur sans aucune affectation sur la période
}
