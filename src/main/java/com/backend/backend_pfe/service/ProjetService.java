package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.ProjetRequestDTO;
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
     *
     * @param request the project creation data
     * @param authentication the security context of the authenticated user
     * @return a DTO containing the created project details
     */
    ProjetResponseDTO creerProjet(ProjetRequestDTO request, Authentication authentication);

    /**
     * Retrieve all projects managed by the authenticated Chef de Projet.
     *
     * @param authentication the security context of the authenticated user
     * @return a list of DTOs containing the project details
     */
    List<ProjetResponseDTO> getMesProjets(Authentication authentication);
}
