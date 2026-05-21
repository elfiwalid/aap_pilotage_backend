package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.ProjetRequestDTO;
import com.backend.backend_pfe.DTO.response.ProjetResponseDTO;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.ProjetRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.StatutProjet;
import com.backend.backend_pfe.exception.BusinessValidationException;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the ProjetService interface.
 *
 * SOLID — Single Responsibility Principle (SRP):
 *   This class contains exclusively the business logic for project creation:
 *   validation rules, repository calls, and DTO mapping.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 *   Depends on repository abstractions (interfaces) injected via constructor.
 */
@Service
@RequiredArgsConstructor
public class ProjetServiceImpl implements ProjetService {

    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;

    @Override
    public ProjetResponseDTO creerProjet(ProjetRequestDTO request, Authentication authentication) {
        // 1. Extraire l'email depuis le contexte d'authentification
        String email = authentication.getName();

        // 2. Rechercher l'utilisateur en base
        User chefProjet = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chef de projet introuvable"));

        // 3. Validation métier : cohérence des dates
        if (request.getDateFin().isBefore(request.getDateDebut())
                || request.getDateFin().isEqual(request.getDateDebut())) {
            throw new BusinessValidationException(
                    "La date de fin doit être postérieure à la date de début");
        }

        // 4. Appliquer le statut par défaut si non fourni
        StatutProjet statut = request.getStatut() != null
                ? request.getStatut()
                : StatutProjet.PLANIFIE;

        // 5. Mapper DTO → Entity
        Projet projet = Projet.builder()
                .nom(request.getNom().trim())
                .description(request.getDescription())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .statut(statut)
                .chefProjet(chefProjet)
                .build();

        // 6. Persister
        Projet saved = projetRepository.save(projet);

        // 7. Mapper Entity → ResponseDTO
        return mapToResponseDTO(saved);
    }

    @Override
    public List<ProjetResponseDTO> getMesProjets(Authentication authentication) {
        String email = authentication.getName();

        User chefProjet = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chef de projet introuvable"));

        List<Projet> projets = projetRepository.findByChefProjet(chefProjet);

        return projets.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private ProjetResponseDTO mapToResponseDTO(Projet projet) {
        User chef = projet.getChefProjet();
        String nomComplet = chef.getNom() + " " + chef.getPrenom();

        return ProjetResponseDTO.builder()
                .id(projet.getId())
                .nom(projet.getNom())
                .description(projet.getDescription())
                .dateDebut(projet.getDateDebut())
                .dateFin(projet.getDateFin())
                .statut(projet.getStatut())
                .chefProjetId(chef.getId())
                .chefProjetNomComplet(nomComplet)
                .dateCreation(projet.getDateCreation())
                .build();
    }
}
