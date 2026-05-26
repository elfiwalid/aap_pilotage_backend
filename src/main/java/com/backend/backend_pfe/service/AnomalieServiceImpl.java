package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.AnomalieResponseDTO;
import com.backend.backend_pfe.Entity.Anomalie;
import com.backend.backend_pfe.Entity.Prevision;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.AffectationRepository;
import com.backend.backend_pfe.Repository.AnomalieRepository;
import com.backend.backend_pfe.Repository.PrevisionRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.StatutAnomalie;
import com.backend.backend_pfe.enums.TypeAnomalie;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnomalieServiceImpl implements AnomalieService {

    private final AnomalieRepository anomalieRepository;
    private final PrevisionRepository previsionRepository;
    private final AffectationRepository affectationRepository;
    private final UserRepository userRepository;

    @Override
    public List<AnomalieResponseDTO> getAnomalies(Authentication authentication,
            TypeAnomalie typeFilter, StatutAnomalie statutFilter) {
        User chefProjet = resolveUser(authentication);

        // Récupérer toutes les anomalies dont le projet appartient au chef connecté
        List<Anomalie> anomalies = anomalieRepository.findAll().stream()
                .filter(a -> a.getProjet().getChefProjet() != null
                        && a.getProjet().getChefProjet().getId().equals(chefProjet.getId()))
                .filter(a -> typeFilter == null || a.getTypeAnomalie() == typeFilter)
                .filter(a -> statutFilter == null || a.getStatut() == statutFilter)
                .toList();

        return anomalies.stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional
    public void resoudreAnomalie(Long anomalieId, Authentication authentication) {
        User chefProjet = resolveUser(authentication);
        Anomalie anomalie = anomalieRepository.findById(anomalieId)
                .orElseThrow(() -> new ResourceNotFoundException("Anomalie introuvable"));

        // Vérifier que le chef est bien propriétaire du projet de l'anomalie
        if (anomalie.getProjet().getChefProjet() == null
                || !anomalie.getProjet().getChefProjet().getId().equals(chefProjet.getId())) {
            throw new AccessDeniedException("Accès refusé");
        }

        anomalie.setStatut(StatutAnomalie.RESOLUE);
        anomalieRepository.save(anomalie);
    }

    private Set<Long> getCollaborateursVisibles(User chefProjet) {
        // Récupérer tous les projets du chef de projet qui ont une prévision active
        List<Prevision> previsionsActives = previsionRepository
                .findByImporteParAndActiveTrue(chefProjet);

        // Récupérer les collaborateurs de ces projets
        return previsionsActives.stream()
                .flatMap(p -> affectationRepository.findByProjet(p.getProjet()).stream())
                .map(a -> a.getCollaborateur().getId())
                .collect(Collectors.toSet());
    }

    private AnomalieResponseDTO mapToDTO(Anomalie anomalie) {
        return AnomalieResponseDTO.builder()
                .id(anomalie.getId())
                .titre(anomalie.getTitre())
                .description(anomalie.getDescription())
                .typeAnomalie(anomalie.getTypeAnomalie().name())
                .statut(anomalie.getStatut().name())
                .dateDetection(anomalie.getDateDetection().toString())
                .resolu(anomalie.getStatut() == StatutAnomalie.RESOLUE)
                .projetId(anomalie.getProjet().getId())
                .projetNom(anomalie.getProjet().getNom())
                .collaborateurId(anomalie.getCollaborateur().getId())
                .collaborateurNomComplet(anomalie.getCollaborateur().getPrenom()
                        + " " + anomalie.getCollaborateur().getNom())
                .build();
    }

    private User resolveUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}
