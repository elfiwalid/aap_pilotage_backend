package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.AnomalieResponseDTO;
import com.backend.backend_pfe.Entity.Affectation;
import com.backend.backend_pfe.Entity.Anomalie;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.AffectationRepository;
import com.backend.backend_pfe.Repository.AnomalieRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import com.backend.backend_pfe.enums.StatutAnomalie;
import com.backend.backend_pfe.enums.TypeAnomalie;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnomalieServiceImpl implements AnomalieService {

    private final AnomalieRepository anomalieRepository;
    private final AffectationRepository affectationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void detecterSurcharge(Long collaborateurId) {
        User user = userRepository.findById(collaborateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborateur non trouvé"));

        List<Affectation> affectations = affectationRepository.findByCollaborateur(user);
        
        double tauxTotal = affectations.stream()
                .mapToDouble(Affectation::getTauxAffectation)
                .sum();

        // Détection : Surcharge (> 100%) ou Sous-utilisation (< 70%)
        if (tauxTotal > 100 || tauxTotal < 70) {
            // Créer l'anomalie si elle n'existe pas déjà pour ce collaborateur (ouverte)
            boolean existeDeja = anomalieRepository.existsByCollaborateurAndStatut(user, StatutAnomalie.OUVERTE);
            
            if (!existeDeja) {
                Anomalie anomalie = new Anomalie();
                anomalie.setDateDetection(LocalDateTime.now());
                anomalie.setStatut(StatutAnomalie.OUVERTE);
                anomalie.setCollaborateur(user);

                if (tauxTotal > 100) {
                    anomalie.setTypeAnomalie(TypeAnomalie.SURCHARGE);
                    anomalie.setTitre("Surcharge de travail");
                    anomalie.setDescription("Le collaborateur " + user.getNom() + " " + user.getPrenom() + " est en surcharge avec un taux de " + tauxTotal + "%.");
                    anomalieRepository.save(anomalie);
                    notificationService.envoyerNotification(user.getId(), "Alerte : Vous êtes en situation de surcharge (" + tauxTotal + "%).");
                } else {
                    anomalie.setTypeAnomalie(TypeAnomalie.TAUX_INCOHERENT);
                    anomalie.setTitre("Sous-utilisation");
                    anomalie.setDescription("Le collaborateur " + user.getNom() + " " + user.getPrenom() + " est en sous-utilisation avec un taux de " + tauxTotal + "%.");
                    anomalieRepository.save(anomalie);
                    notificationService.envoyerNotification(user.getId(), "Alerte : Vous êtes en situation de sous-utilisation (" + tauxTotal + "%).");
                }
            }
        }
    }

    @Override
    public List<AnomalieResponseDTO> getAllAnomalies() {
        return anomalieRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void resoudreAnomalie(Long id) {
        Anomalie anomalie = anomalieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anomalie non trouvée"));
        anomalie.setStatut(StatutAnomalie.RESOLUE);
        anomalieRepository.save(anomalie);
    }

    private AnomalieResponseDTO mapToResponseDTO(Anomalie an) {
        return AnomalieResponseDTO.builder()
                .id(an.getId())
                .typeAnomalie(an.getTypeAnomalie() != null ? an.getTypeAnomalie().name() : "N/A")
                .description(an.getDescription())
                .dateDetection(an.getDateDetection())
                .resolu(an.getStatut() == StatutAnomalie.RESOLUE)
                .collaborateurId(an.getCollaborateur().getId())
                .collaborateurNomComplet(an.getCollaborateur().getNom() + " " + an.getCollaborateur().getPrenom())
                .projetId(an.getProjet() != null ? an.getProjet().getId() : null)
                .projetNom(an.getProjet() != null ? an.getProjet().getNom() : "N/A")
                .build();
    }
}
