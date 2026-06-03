package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.response.CollabDashboardDTO;
import com.backend.backend_pfe.DTO.response.CollabPlanningJourDTO;
import com.backend.backend_pfe.DTO.response.CollabProjetDTO;
import com.backend.backend_pfe.service.CollaborateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller pour l'espace collaborateur.
 *
 * SOLID — SRP : ne gère que les concerns HTTP, délègue au service.
 */
@RestController
@RequestMapping("/api/collaborateur")
@RequiredArgsConstructor
public class CollaborateurController {

    private final CollaborateurService collaborateurService;

    /** GET /api/collaborateur/dashboard — KPIs, charge mensuelle, projets. */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('COLLABORATEUR')")
    public ResponseEntity<CollabDashboardDTO> getDashboard(Authentication authentication) {
        return ResponseEntity.ok(collaborateurService.getDashboard(authentication));
    }

    /** GET /api/collaborateur/projets — Projets assignés au collaborateur. */
    @GetMapping("/projets")
    @PreAuthorize("hasRole('COLLABORATEUR')")
    public ResponseEntity<List<CollabProjetDTO>> getMesProjets(Authentication authentication) {
        return ResponseEntity.ok(collaborateurService.getMesProjets(authentication));
    }

    /**
     * GET /api/collaborateur/planning?annee=2026&mois=4 — Planning du mois.
     * Si annee/mois non fournis, utilise le mois courant.
     */
    @GetMapping("/planning")
    @PreAuthorize("hasRole('COLLABORATEUR')")
    public ResponseEntity<List<CollabPlanningJourDTO>> getPlanning(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois,
            Authentication authentication) {
        LocalDate now = LocalDate.now();
        int y = annee != null ? annee : now.getYear();
        int m = mois != null ? mois : now.getMonthValue();
        return ResponseEntity.ok(collaborateurService.getPlanning(authentication, y, m));
    }
}
