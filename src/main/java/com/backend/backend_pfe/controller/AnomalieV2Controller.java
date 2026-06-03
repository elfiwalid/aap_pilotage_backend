package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.Entity.AnomalieV2;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.AffectationRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.StatutAnomalieV2;
import com.backend.backend_pfe.enums.TypeAnomalieV2;
import com.backend.backend_pfe.service.AnomalieDetectionV2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller pour la détection et gestion des anomalies V2.
 */
@RestController
@RequestMapping("/api/rm/anomalies-v2")
@RequiredArgsConstructor
public class AnomalieV2Controller {

    private final AnomalieDetectionV2Service detectionService;
    private final UserRepository userRepository;
    private final AffectationRepository affectationRepository;

    /** POST /api/rm/anomalies-v2/detecter — Lancer la détection pour un mois. */
    @PostMapping("/detecter")
    @PreAuthorize("hasRole('RESOURCE_MANAGER')")
    public ResponseEntity<List<AnomalieV2>> detecter(
            @RequestParam int annee,
            @RequestParam int mois,
            @RequestParam(defaultValue = "ma") String pays) {
        return ResponseEntity.ok(detectionService.detecterAnomalies(annee, mois, pays));
    }

    /** GET /api/rm/anomalies-v2 — Lister les anomalies d'une période. */
    @GetMapping
    @PreAuthorize("hasAnyRole('RESOURCE_MANAGER', 'CHEF_PROJET')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AnomalieV2>> lister(
            @RequestParam int annee,
            @RequestParam int mois,
            @RequestParam(required = false) TypeAnomalieV2 type) {
        if (type != null) {
            return ResponseEntity.ok(detectionService.getAnomaliesByType(annee, mois, type));
        }
        return ResponseEntity.ok(detectionService.getAnomalies(annee, mois));
    }

    /** GET /api/rm/anomalies-v2/par-chef — Anomalies filtrées par chef de projet (ses collaborateurs uniquement). */
    @GetMapping("/par-chef")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AnomalieV2>> listerParChef(
            @RequestParam int annee,
            @RequestParam int mois,
            Authentication authentication) {
        String email = authentication.getName();
        User chef = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return ResponseEntity.ok(detectionService.getAnomaliesParChef(annee, mois, chef.getId()));
    }

    /** GET /api/rm/anomalies-v2/periodes — Retourne les périodes qui ont des anomalies. */
    @GetMapping("/periodes")
    @PreAuthorize("hasAnyRole('RESOURCE_MANAGER', 'CHEF_PROJET')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Integer>>> getPeriodes() {
        return ResponseEntity.ok(
            detectionService.getPeriodesDisponibles().stream()
                .map(arr -> Map.of("annee", arr[0], "mois", arr[1]))
                .toList()
        );
    }

    /** GET /api/rm/anomalies-v2/periodes-chef — Périodes avec anomalies pour le chef connecté. */
    @GetMapping("/periodes-chef")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Integer>>> getPeriodesChef(Authentication authentication) {
        String email = authentication.getName();
        User chef = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return ResponseEntity.ok(
            detectionService.getPeriodesDisponiblesParChef(chef.getId()).stream()
                .map(arr -> Map.of("annee", arr[0], "mois", arr[1]))
                .toList()
        );
    }

    /** GET /api/rm/anomalies-v2/{id} — Détail d'une anomalie. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESOURCE_MANAGER', 'CHEF_PROJET')")
    @Transactional(readOnly = true)
    public ResponseEntity<AnomalieV2> detail(@PathVariable Long id) {
        return ResponseEntity.ok(detectionService.getAnomalie(id));
    }

    /** PUT /api/rm/anomalies-v2/{id}/statut — Changer le statut. */
    @PutMapping("/{id}/statut")
    @PreAuthorize("hasRole('RESOURCE_MANAGER')")
    public ResponseEntity<Void> changerStatut(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        StatutAnomalieV2 statut = StatutAnomalieV2.valueOf(body.get("statut"));
        detectionService.changerStatut(id, statut);
        return ResponseEntity.ok().build();
    }

    /** GET /api/rm/anomalies-v2/taux-charge — Taux de charge d'un collaborateur. */
    @GetMapping("/taux-charge")
    @PreAuthorize("hasRole('RESOURCE_MANAGER')")
    public ResponseEntity<Map<String, Double>> getTauxCharge(
            @RequestParam Long collaborateurId,
            @RequestParam int annee,
            @RequestParam int mois) {
        double taux = detectionService.getTauxCharge(collaborateurId, annee, mois);
        return ResponseEntity.ok(Map.of("tauxCharge", taux));
    }

    /** GET /api/rm/anomalies-v2/affectations — Affectations d'un collaborateur pour un mois (dates exactes). */
    @GetMapping("/affectations")
    @PreAuthorize("hasAnyRole('RESOURCE_MANAGER', 'CHEF_PROJET')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAffectationsCollab(
            @RequestParam String matricule,
            @RequestParam int annee,
            @RequestParam int mois) {
        User collab = userRepository.findByMatricule(matricule)
                .orElseThrow(() -> new RuntimeException("Collaborateur introuvable"));

        java.time.YearMonth ym = java.time.YearMonth.of(annee, mois);
        java.time.LocalDate monthStart = ym.atDay(1);
        java.time.LocalDate monthEnd = ym.atEndOfMonth();

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (var aff : affectationRepository.findByCollaborateur(collab)) {
            if (aff.getDateDebut() == null || aff.getDateFin() == null) continue;
            if (aff.getDateFin().isBefore(monthStart) || aff.getDateDebut().isAfter(monthEnd)) continue;
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("projetNom", aff.getProjet().getNom());
            m.put("dateDebut", aff.getDateDebut().toString());
            m.put("dateFin", aff.getDateFin().toString());
            m.put("tauxAffectation", aff.getTauxAffectation());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }
}
