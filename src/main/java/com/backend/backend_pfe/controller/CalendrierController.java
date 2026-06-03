package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.response.CalendrierConfigDTO;
import com.backend.backend_pfe.service.CalendrierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller pour la configuration du calendrier (jours ouvrables, fériés).
 */
@RestController
@RequestMapping("/api/rm/calendrier")
@RequiredArgsConstructor
public class CalendrierController {

    private final CalendrierService calendrierService;

    /**
     * GET /api/rm/calendrier?pays=ma&annee=2026
     * Retourne la config calendrier avec jours fériés (API Nager.Date) et jours ouvrables calculés.
     */
    @GetMapping
    @PreAuthorize("hasRole('RESOURCE_MANAGER')")
    public ResponseEntity<CalendrierConfigDTO> getCalendrier(
            @RequestParam(defaultValue = "ma") String pays,
            @RequestParam(defaultValue = "2026") int annee) {
        return ResponseEntity.ok(calendrierService.getCalendrier(pays, annee));
    }
}
