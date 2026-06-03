package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.CalendrierConfigDTO;

/**
 * Service pour la gestion du calendrier et des jours ouvrables.
 */
public interface CalendrierService {

    /**
     * Récupère la configuration du calendrier pour un pays et une année.
     * Appelle l'API Nager.Date pour les jours fériés.
     */
    CalendrierConfigDTO getCalendrier(String pays, int annee);
}
