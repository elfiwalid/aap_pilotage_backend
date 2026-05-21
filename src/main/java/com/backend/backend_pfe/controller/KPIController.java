package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.service.KPIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/kpis")
@RequiredArgsConstructor
public class KPIController {

    private final KPIService kpiService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(Map.of(
            "tauxOccupation", kpiService.calculerTauxOccupationGlobal(),
            "tnf", kpiService.calculerTNF(),
            "occupationParCollab", kpiService.getOccupationParCollaborateur(),
            "evolution", kpiService.getEvolutionOccupationMensuelle()
        ));
    }
}
