package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.EvaluationRequestDTO;
import com.backend.backend_pfe.DTO.response.EvaluationResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Service interface for performance evaluation operations.
 *
 * SOLID — DIP: controllers depend on this abstraction.
 * SOLID — ISP: only evaluation-related methods are declared here.
 */
public interface EvaluationService {

    /**
     * Submit a performance evaluation (Chef de Projet → Collaborateur).
     * Updates existing evaluation if one already exists for the same month/year/collab.
     */
    EvaluationResponseDTO evaluerCollaborateur(Authentication authentication, EvaluationRequestDTO request);

    /**
     * Get all evaluations received by the currently authenticated collaborateur.
     */
    List<EvaluationResponseDTO> getMesEvaluations(Authentication authentication);

    /**
     * Get all evaluations given by the currently authenticated chef de projet.
     */
    List<EvaluationResponseDTO> getEvaluationsParChef(Authentication authentication);

    /**
     * Get all evaluations for a specific collaborateur (by ID).
     */
    List<EvaluationResponseDTO> getEvaluationsCollaborateur(Long collaborateurId);
}
