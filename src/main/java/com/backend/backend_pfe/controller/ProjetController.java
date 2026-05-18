package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.request.ProjetRequestDTO;
import com.backend.backend_pfe.DTO.response.ProjetResponseDTO;
import com.backend.backend_pfe.service.ProjetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for project creation endpoints.
 *
 * SOLID — Single Responsibility Principle (SRP):
 * This controller only handles HTTP concerns (request mapping,
 * validation, response formatting). All business logic is
 * delegated to ProjetService.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 * Depends on the ProjetService abstraction, not on ProjetServiceImpl.
 *
 * Clean Code — Thin Controller:
 * The controller is intentionally thin; it validates input
 * and delegates to the service layer.
 */
@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
public class ProjetController {

    private final ProjetService projetService;

    /**
     * Create a new project associated with the authenticated Chef de Projet.
     *
     * @param request        ProjetRequestDTO with project details
     * @param authentication the security context of the authenticated user
     * @return ProjetResponseDTO containing the created project details
     */
    @PostMapping
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<ProjetResponseDTO> creerProjet(
            @Valid @RequestBody ProjetRequestDTO request,
            Authentication authentication) {
        ProjetResponseDTO response = projetService.creerProjet(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all projects managed by the authenticated Chef de Projet.
     *
     * @param authentication the security context of the authenticated user
     * @return list of ProjetResponseDTO
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'RESOURCE_MANAGER')")
    public ResponseEntity<List<ProjetResponseDTO>> getMesProjets(Authentication authentication) {
        List<ProjetResponseDTO> projets = projetService.getMesProjets(authentication);
        return ResponseEntity.ok(projets);
    }
}
