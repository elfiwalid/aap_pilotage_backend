package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.request.SimulationRemplacementRequestDTO;
import com.backend.backend_pfe.DTO.request.SimulationSousChargeRequestDTO;
import com.backend.backend_pfe.DTO.response.SimulationRemplacementResponseDTO;
import com.backend.backend_pfe.DTO.response.SimulationSousChargeResponseDTO;
import com.backend.backend_pfe.service.WhatIfSimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/simulations/what-if")
@RestController
@RequiredArgsConstructor
public class WhatIfSimulationController {

    private final WhatIfSimulationService whatIfSimulationService;

    @PostMapping("/remplacement")
    public ResponseEntity<SimulationRemplacementResponseDTO> simulerRemplacement(
            @Valid @RequestBody SimulationRemplacementRequestDTO request
    ) {
        return ResponseEntity.ok(whatIfSimulationService.simulerRemplacement(request));
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
}
