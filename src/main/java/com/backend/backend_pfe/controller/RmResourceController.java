package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.response.RmConflitDTO;
import com.backend.backend_pfe.DTO.response.RmDashboardDTO;
import com.backend.backend_pfe.DTO.response.RmProjetDTO;
import com.backend.backend_pfe.DTO.response.RmResourceDTO;
import com.backend.backend_pfe.service.ExportV2Service;
import com.backend.backend_pfe.service.RmResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller pour la gestion des ressources et projets (vue Resource Manager).
 */
@RestController
@RequestMapping("/api/rm")
@RequiredArgsConstructor
public class RmResourceController {

    private final RmResourceService rmResourceService;
    private final ExportV2Service exportV2Service;

    /** GET /api/rm/resources — Liste de tous les collaborateurs avec taux et heatmap. */
    @GetMapping("/resources")
    @PreAuthorize("hasRole('RESOURCE_MANAGER')")
    public ResponseEntity<List<RmResourceDTO>> getAllResources(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois) {
        return ResponseEntity.ok(rmResourceService.getAllResources(annee, mois));
    }

    /** GET /api/rm/dashboard — Données agrégées du dashboard RM. */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('RESOURCE_MANAGER')")
    public ResponseEntity<RmDashboardDTO> getDashboard() {
        return ResponseEntity.ok(rmResourceService.getDashboard());
    }

    /** GET /api/rm/projets — Liste de tous les projets avec leur équipe. */
    @GetMapping("/projets")
    @PreAuthorize("hasRole('RESOURCE_MANAGER')")
    public ResponseEntity<List<RmProjetDTO>> getAllProjets() {
        return ResponseEntity.ok(rmResourceService.getAllProjets());
    }

    /** GET /api/rm/conflits — Liste des anomalies enrichies avec alternatives. */
    @GetMapping("/conflits")
    @PreAuthorize("hasRole('RESOURCE_MANAGER')")
    public ResponseEntity<List<RmConflitDTO>> getConflits() {
        return ResponseEntity.ok(rmResourceService.getConflits());
    }

    /** POST /api/rm/propositions — Proposer un collaborateur alternatif au chef de projet. */
    @PostMapping("/propositions")
    @PreAuthorize("hasRole('RESOURCE_MANAGER')")
    public ResponseEntity<Void> proposerAlternative(
            @RequestBody java.util.Map<String, Long> body,
            org.springframework.security.core.Authentication authentication) {
        rmResourceService.proposerAlternative(
                body.get("anomalieId"),
                body.get("collaborateurId"),
                body.get("projetId"),
                authentication);
        return ResponseEntity.ok().build();
    }

    /** POST /api/rm/export-v2 — Exporter le V2 consolidé pour les projets sélectionnés. */
    @PostMapping("/export-v2")
    @PreAuthorize("hasRole('RESOURCE_MANAGER')")
    public ResponseEntity<byte[]> exporterV2(@RequestBody java.util.Map<String, java.util.List<Long>> body) {
        java.util.List<Long> projetIds = body.get("projetIds");
        byte[] file = exportV2Service.exporterV2Consolide(projetIds);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"V2_Consolide_" + java.time.LocalDate.now() + ".xlsx\"")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(file);
    }
}
