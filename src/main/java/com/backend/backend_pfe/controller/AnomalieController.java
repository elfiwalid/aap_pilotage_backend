package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.response.AnomalieResponseDTO;
import com.backend.backend_pfe.enums.StatutAnomalie;
import com.backend.backend_pfe.enums.TypeAnomalie;
import com.backend.backend_pfe.service.AnomalieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for anomaly consultation and resolution endpoints.
 *
 * SOLID — Single Responsibility Principle (SRP):
 *   This controller only handles HTTP concerns (request mapping,
 *   parameter binding, response formatting). All business logic is
 *   delegated to AnomalieService.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 *   Depends on the AnomalieService abstraction, not on AnomalieServiceImpl.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnomalieController {

    private final AnomalieService anomalieService;

    /**
     * GET /api/anomalies — Liste des anomalies du chef de projet authentifié.
     * Filtres optionnels: typeAnomalie, statut.
     *
     * @param typeAnomalie optional filter by anomaly type
     * @param statut optional filter by anomaly status
     * @param authentication the security context of the authenticated user
     * @return list of AnomalieResponseDTO
     */
    @GetMapping("/anomalies")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<List<AnomalieResponseDTO>> getAnomalies(
            @RequestParam(required = false) TypeAnomalie typeAnomalie,
            @RequestParam(required = false) StatutAnomalie statut,
            Authentication authentication) {
        List<AnomalieResponseDTO> anomalies = anomalieService.getAnomalies(
                authentication, typeAnomalie, statut);
        return ResponseEntity.ok(anomalies);
    }

    /**
     * PUT /api/anomalies/{id}/resoudre — Marquer une anomalie comme résolue.
     *
     * @param id the ID of the anomaly to resolve
     * @param authentication the security context of the authenticated user
     * @return HTTP 200 with empty body
     */
    @PutMapping("/anomalies/{id}/resoudre")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<Void> resoudreAnomalie(
            @PathVariable Long id,
            Authentication authentication) {
        anomalieService.resoudreAnomalie(id, authentication);
        return ResponseEntity.ok().build();
    }
}
