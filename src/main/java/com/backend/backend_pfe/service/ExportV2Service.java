package com.backend.backend_pfe.service;

import java.util.List;

/**
 * Service pour l'export V2 consolidé en format Excel.
 */
public interface ExportV2Service {
    /**
     * Génère un fichier Excel V2 consolidé pour les projets sélectionnés.
     * @param projetIds liste des IDs de projets à inclure
     * @return le contenu du fichier Excel en bytes
     */
    byte[] exporterV2Consolide(List<Long> projetIds);
}
