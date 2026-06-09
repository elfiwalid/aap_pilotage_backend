package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.ProjetRequestDTO;
import com.backend.backend_pfe.DTO.response.DashboardChefProjetDTO;
import com.backend.backend_pfe.DTO.response.ProjetResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Service contract for project management operations.
 *
 * SOLID — Interface Segregation Principle (ISP):
 *   Only project-related operations are defined here.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 *   The controller depends on this abstraction, not on the implementation.
 */
public interface ProjetService {

    /**
     * Create a new project and associate it with the authenticated Chef de Projet.
     */
    ProjetResponseDTO creerProjet(ProjetRequestDTO request, Authentication authentication);

    /**
     * Retrieve all projects managed by the authenticated Chef de Projet.
     */
    List<ProjetResponseDTO> getMesProjets(Authentication authentication);

    /**
     * Compute the full dashboard KPIs for the authenticated Chef de Projet.
     * If annee/mois are null, uses current month.
     */
    DashboardChefProjetDTO getDashboard(Authentication authentication, Integer annee, Integer mois);

    /**
     * Delete a project owned by the authenticated Chef de Projet.
     */
    void supprimerProjet(Long projetId, Authentication authentication);
}
