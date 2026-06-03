package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.request.EvaluationRequestDTO;
import com.backend.backend_pfe.DTO.response.EvaluationResponseDTO;
import com.backend.backend_pfe.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for performance evaluation endpoints.
 *
 * SOLID — SRP: handles only HTTP concerns for evaluations.
 * SOLID — DIP: depends on the EvaluationService abstraction.
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    /**
     * POST /api/evaluations — Submit a performance evaluation.
     * Only accessible to Chef de Projet.
     */
    @PostMapping
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<EvaluationResponseDTO> evaluer(
            Authentication authentication,
            @Valid @RequestBody EvaluationRequestDTO request) {
        return ResponseEntity.ok(evaluationService.evaluerCollaborateur(authentication, request));
    }

    /**
     * GET /api/evaluations/mes-evaluations — Get evaluations received by the current user.
     * Accessible to all authenticated users (primarily for collaborateurs).
     */
    @GetMapping("/mes-evaluations")
    public ResponseEntity<List<EvaluationResponseDTO>> getMesEvaluations(Authentication authentication) {
        return ResponseEntity.ok(evaluationService.getMesEvaluations(authentication));
    }

    /**
     * GET /api/evaluations/par-chef — Get evaluations given by the current chef de projet.
     * Only accessible to Chef de Projet.
     */
    @GetMapping("/par-chef")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<List<EvaluationResponseDTO>> getEvaluationsParChef(Authentication authentication) {
        return ResponseEntity.ok(evaluationService.getEvaluationsParChef(authentication));
    }

    /**
     * GET /api/evaluations/collaborateur/{id} — Get evaluations for a specific collaborateur.
     * Accessible to Chef de Projet and Resource Manager.
     */
    @GetMapping("/collaborateur/{id}")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'RESOURCE_MANAGER')")
    public ResponseEntity<List<EvaluationResponseDTO>> getEvaluationsCollaborateur(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.getEvaluationsCollaborateur(id));
    }
}
