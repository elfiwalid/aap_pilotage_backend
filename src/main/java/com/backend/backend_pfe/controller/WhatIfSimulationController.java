package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.request.SimulationRemplacementRequestDTO;
import com.backend.backend_pfe.DTO.request.SimulationDepuisConflitRequestDTO;
import com.backend.backend_pfe.DTO.request.SimulationDecisionRequest;
import com.backend.backend_pfe.DTO.request.SimulationSousChargeRequestDTO;
import com.backend.backend_pfe.DTO.response.CollaborateurDisponibleConflitDTO;
import com.backend.backend_pfe.DTO.response.ConversationSimulationDTO;
import com.backend.backend_pfe.DTO.response.SimulationConflitContextDTO;
import com.backend.backend_pfe.DTO.response.SimulationRemplacementResponseDTO;
import com.backend.backend_pfe.DTO.response.SimulationSousChargeResponseDTO;
import com.backend.backend_pfe.service.SimulationValidationService;
import com.backend.backend_pfe.service.WhatIfSimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/simulations/what-if")
@RestController
@RequiredArgsConstructor
public class WhatIfSimulationController {

    private final WhatIfSimulationService whatIfSimulationService;
    private final SimulationValidationService simulationValidationService;

    @PostMapping("/remplacement")
    public ResponseEntity<SimulationRemplacementResponseDTO> simulerRemplacement(
            @Valid @RequestBody SimulationRemplacementRequestDTO request
    ) {
        return ResponseEntity.ok(whatIfSimulationService.simulerRemplacement(request));
    }

    @GetMapping("/conflits/{conflitId}/context")
    public ResponseEntity<SimulationConflitContextDTO> getConflitContext(@PathVariable Long conflitId) {
        return ResponseEntity.ok(whatIfSimulationService.getConflitContext(conflitId));
    }

    @GetMapping("/conflits/{conflitId}/collaborateurs-disponibles")
    public ResponseEntity<List<CollaborateurDisponibleConflitDTO>> getCollaborateursDisponibles(
            @PathVariable Long conflitId
    ) {
        return ResponseEntity.ok(whatIfSimulationService.getCollaborateursDisponiblesPourConflit(conflitId));
    }

    @PostMapping("/from-conflit")
    public ResponseEntity<SimulationRemplacementResponseDTO> simulerDepuisConflit(
            @Valid @RequestBody SimulationDepuisConflitRequestDTO request
    ) {
        return ResponseEntity.ok(whatIfSimulationService.simulerDepuisConflit(request));
    }

    @PostMapping("/sous-charge")
    public ResponseEntity<SimulationSousChargeResponseDTO> simulerSousCharge(
            @Valid @RequestBody SimulationSousChargeRequestDTO request
    ) {
        return ResponseEntity.ok(
                whatIfSimulationService.simulerSousCharge(request)
        );
    }

    @PostMapping("/{simulationId}/valider")
    public ResponseEntity<Void> validerSimulation(@PathVariable Long simulationId) {
        whatIfSimulationService.validerSimulation(simulationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{simulationId}/annuler")
    public ResponseEntity<Void> annulerSimulation(@PathVariable Long simulationId) {
        whatIfSimulationService.annulerSimulation(simulationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{simulationId}/accept")
    public ResponseEntity<ConversationSimulationDTO> acceptSimulation(
            @PathVariable Long simulationId,
            @RequestBody(required = false) SimulationDecisionRequest request,
            Authentication authentication
    ) {
        String commentaire = request != null ? request.getCommentaire() : null;
        return ResponseEntity.ok(simulationValidationService.acceptSimulation(
                simulationId,
                commentaire,
                authentication
        ));
    }

    @PostMapping("/{simulationId}/reject")
    public ResponseEntity<ConversationSimulationDTO> rejectSimulation(
            @PathVariable Long simulationId,
            @RequestBody(required = false) SimulationDecisionRequest request,
            Authentication authentication
    ) {
        String commentaire = request != null ? request.getCommentaire() : null;
        return ResponseEntity.ok(simulationValidationService.rejectSimulation(
                simulationId,
                commentaire,
                authentication
        ));
    }
}
