package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.request.AffectationRequestDTO;
import com.backend.backend_pfe.DTO.response.AffectationResponseDTO;
import com.backend.backend_pfe.service.AffectationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/affectations")
@RequiredArgsConstructor
public class AffectationController {

    private final AffectationService affectationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RESOURCE_MANAGER', 'CHEF_PROJET')")
    public ResponseEntity<AffectationResponseDTO> creerAffectation(@Valid @RequestBody AffectationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(affectationService.creerAffectation(request));
    }

    @GetMapping("/projet/{projetId}")
    public ResponseEntity<List<AffectationResponseDTO>> getParProjet(@PathVariable Long projetId) {
        return ResponseEntity.ok(affectationService.getAffectationsParProjet(projetId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESOURCE_MANAGER', 'CHEF_PROJET')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        affectationService.supprimerAffectation(id);
        return ResponseEntity.noContent().build();
    }
}
