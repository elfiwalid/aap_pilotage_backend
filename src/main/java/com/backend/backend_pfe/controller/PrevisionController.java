package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.response.PrevisionResponseDTO;
import com.backend.backend_pfe.DTO.response.PrevisionStatsDTO;
import com.backend.backend_pfe.enums.TypePrevision;
import com.backend.backend_pfe.service.PrevisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for prevision (forecast) management endpoints.
 *
 * SOLID — Single Responsibility Principle (SRP):
 *   This controller only handles HTTP concerns (request mapping,
 *   validation, response formatting). All business logic is
 *   delegated to PrevisionService.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 *   Depends on the PrevisionService abstraction, not on PrevisionServiceImpl.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PrevisionController {

    private final PrevisionService previsionService;

    /**
     * Import a new prevision Excel file for a project.
     *
     * @param projetId the ID of the target project
     * @param file the Excel file to import (.xlsx or .xls)
     * @param typePrevision the type of prevision (TRIMESTRIELLE or ANNUELLE)
     * @param authentication the security context of the authenticated user
     * @return PrevisionResponseDTO containing the created prevision details
     */
    @PostMapping("/projets/{projetId}/previsions")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<PrevisionResponseDTO> importerPrevision(
            @PathVariable Long projetId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("typePrevision") TypePrevision typePrevision,
            @RequestParam("periodeDebut") String periodeDebutStr,
            @RequestParam("periodeFin") String periodeFinStr,
            Authentication authentication) {
        java.time.LocalDate periodeDebut = java.time.LocalDate.parse(periodeDebutStr);
        java.time.LocalDate periodeFin = java.time.LocalDate.parse(periodeFinStr);
        PrevisionResponseDTO response = previsionService.importerPrevision(
                projetId, file, typePrevision, periodeDebut, periodeFin, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve the history of all previsions for a project, ordered by import date descending.
     *
     * @param projetId the ID of the target project
     * @param authentication the security context of the authenticated user
     * @return list of PrevisionResponseDTO
     */
    @GetMapping("/projets/{projetId}/previsions")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<List<PrevisionResponseDTO>> getHistorique(
            @PathVariable Long projetId,
            Authentication authentication) {
        List<PrevisionResponseDTO> historique = previsionService.getHistorique(
                projetId, authentication);
        return ResponseEntity.ok(historique);
    }

    /**
     * Retrieve the currently active prevision for a project.
     * Returns HTTP 200 with the active prevision, or HTTP 204 if none exists.
     *
     * @param projetId the ID of the target project
     * @param authentication the security context of the authenticated user
     * @return PrevisionResponseDTO or no content
     */
    @GetMapping("/projets/{projetId}/previsions/active")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<PrevisionResponseDTO> getPrevisionActive(
            @PathVariable Long projetId,
            Authentication authentication) {
        return previsionService.getPrevisionActive(projetId, authentication)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Download the Excel file of a specific prevision.
     *
     * @param previsionId the ID of the prevision to download
     * @param authentication the security context of the authenticated user
     * @return the file bytes with appropriate headers
     */
    @GetMapping("/previsions/{previsionId}/download")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<byte[]> telechargerPrevision(
            @PathVariable Long previsionId,
            Authentication authentication) {
        return previsionService.telechargerPrevision(previsionId, authentication);
    }

    /**
     * Retrieve statistics for a specific prevision.
     *
     * @param previsionId the ID of the prevision
     * @param authentication the security context of the authenticated user
     * @return PrevisionStatsDTO containing the prevision statistics
     */
    @GetMapping("/previsions/{previsionId}/stats")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<PrevisionStatsDTO> getStatistiques(
            @PathVariable Long previsionId,
            Authentication authentication) {
        PrevisionStatsDTO stats = previsionService.getStatistiques(
                previsionId, authentication);
        return ResponseEntity.ok(stats);
    }
}
