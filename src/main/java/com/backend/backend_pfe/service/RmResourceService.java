package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.RmConflitDTO;
import com.backend.backend_pfe.DTO.response.RmDashboardDTO;
import com.backend.backend_pfe.DTO.response.RmProjetDTO;
import com.backend.backend_pfe.DTO.response.RmResourceDTO;

import java.util.List;

/**
 * Service pour la vue Resource Manager — gestion des ressources et projets.
 */
public interface RmResourceService {
    List<RmResourceDTO> getAllResources(Integer annee, Integer mois);
    List<RmProjetDTO> getAllProjets();
    List<RmConflitDTO> getConflits();
    RmDashboardDTO getDashboard(Integer annee, Integer mois);
    void proposerAlternative(Long anomalieId, Long collaborateurId, Long projetId,
                             org.springframework.security.core.Authentication authentication);
}
