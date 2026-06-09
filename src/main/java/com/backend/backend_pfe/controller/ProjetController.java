package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.request.ProjetRequestDTO;
import com.backend.backend_pfe.DTO.response.DashboardChefProjetDTO;
import com.backend.backend_pfe.DTO.response.PmRapportMensuelDTO;
import com.backend.backend_pfe.DTO.response.ProjetResponseDTO;
import com.backend.backend_pfe.service.PmRapportV2Service;
import com.backend.backend_pfe.service.ProjetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for project creation endpoints.
 *
 * SOLID — Single Responsibility Principle (SRP):
 *   This controller only handles HTTP concerns (request mapping,
 *   validation, response formatting). All business logic is
 *   delegated to ProjetService.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 *   Depends on the ProjetService abstraction, not on ProjetServiceImpl.
 *
 * Clean Code — Thin Controller:
 *   The controller is intentionally thin; it validates input
 *   and delegates to the service layer.
 */
@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
public class ProjetController {

    private final ProjetService projetService;
    private final PmRapportV2Service pmRapportV2Service;

    /**
     * Create a new project associated with the authenticated Chef de Projet.
     *
     * @param request ProjetRequestDTO with project details
     * @param authentication the security context of the authenticated user
     * @return ProjetResponseDTO containing the created project details
     */
    @PostMapping
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<ProjetResponseDTO> creerProjet(
            @Valid @RequestBody ProjetRequestDTO request,
            Authentication authentication) {
        ProjetResponseDTO response = projetService.creerProjet(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all projects managed by the authenticated Chef de Projet.
     *
     * @param authentication the security context of the authenticated user
     * @return list of ProjetResponseDTO
     */
    @GetMapping
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<List<ProjetResponseDTO>> getMesProjets(Authentication authentication) {
        List<ProjetResponseDTO> projets = projetService.getMesProjets(authentication);
        return ResponseEntity.ok(projets);
    }

    /**
     * Tableau de bord complet du Chef de Projet.
     * Retourne tous les KPIs (projets, collaborateurs, anomalies, graphiques)
     * en un seul appel API.
     *
     * @param authentication the security context of the authenticated user
     * @return DashboardChefProjetDTO containing all KPIs
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<DashboardChefProjetDTO> getDashboard(
            Authentication authentication,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois) {
        return ResponseEntity.ok(projetService.getDashboard(authentication, annee, mois));
    }

    /**
     * Rapports V2 mensuels dynamiques du Chef de Projet connecte.
     * Les rapports sont generes a la volee depuis les previsions, affectations
     * et anomalies V2 existantes, sans persistance dans RapportV2.
     */
    @GetMapping("/rapports-v2")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<List<PmRapportMensuelDTO>> getRapportsV2(Authentication authentication) {
        return ResponseEntity.ok(pmRapportV2Service.getRapportsMensuels(authentication));
    }

    @DeleteMapping("/{projetId}")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<Void> supprimerProjet(
            @PathVariable Long projetId,
            Authentication authentication) {
        projetService.supprimerProjet(projetId, authentication);
        return ResponseEntity.noContent().build();
    }
}
