package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.response.ImportTachesResponseDTO;
import com.backend.backend_pfe.DTO.response.TacheCollaborateurDTO;
import com.backend.backend_pfe.service.TacheCollaborateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TacheCollaborateurController {

    private final TacheCollaborateurService tacheCollaborateurService;

    @PostMapping("/projets/{projetId}/taches/import")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<ImportTachesResponseDTO> importerTaches(
            @PathVariable Long projetId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        ImportTachesResponseDTO response =
                tacheCollaborateurService.importerTaches(projetId, file, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/collaborateur/taches")
    @PreAuthorize("hasRole('COLLABORATEUR')")
    public ResponseEntity<List<TacheCollaborateurDTO>> getMesTaches(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois,
            Authentication authentication) {
        return ResponseEntity.ok(
                tacheCollaborateurService.getTachesCollaborateur(authentication, annee, mois));
    }

    @GetMapping("/collaborateur/taches/planning")
    @PreAuthorize("hasRole('COLLABORATEUR')")
    public ResponseEntity<List<TacheCollaborateurDTO>> getMesTachesPlanning(
            @RequestParam Integer annee,
            @RequestParam Integer mois,
            Authentication authentication) {
        return ResponseEntity.ok(
                tacheCollaborateurService.getTachesCollaborateur(authentication, annee, mois));
    }

    @GetMapping("/projets/{projetId}/taches")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<List<TacheCollaborateurDTO>> getTachesProjet(
            @PathVariable Long projetId,
            Authentication authentication) {
        return ResponseEntity.ok(tacheCollaborateurService.getTachesProjet(projetId, authentication));
    }
}
