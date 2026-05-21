package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.response.AnomalieResponseDTO;
import com.backend.backend_pfe.service.AnomalieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class AnomalieController {

    private final AnomalieService anomalieService;

    @GetMapping
    public ResponseEntity<List<AnomalieResponseDTO>> getAll() {
        return ResponseEntity.ok(anomalieService.getAllAnomalies());
    }

    @PutMapping("/{id}/resoudre")
    public ResponseEntity<Void> resoudre(@PathVariable Long id) {
        anomalieService.resoudreAnomalie(id);
        return ResponseEntity.ok().build();
    }
}
