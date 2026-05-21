package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.AffectationRequestDTO;
import com.backend.backend_pfe.DTO.response.AffectationResponseDTO;
import com.backend.backend_pfe.Entity.Affectation;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.AffectationRepository;
import com.backend.backend_pfe.Repository.ProjetRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.exception.BusinessValidationException;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AffectationServiceImpl implements AffectationService {

    private final AffectationRepository affectationRepository;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    private final AnomalieService anomalieService;

    @Override
    @Transactional
    public AffectationResponseDTO creerAffectation(AffectationRequestDTO request) {
        Projet projet = projetRepository.findById(request.getProjetId())
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));

        User collaborateur = userRepository.findById(request.getCollaborateurId())
                .orElseThrow(() -> new ResourceNotFoundException("Collaborateur non trouvé"));

        if (request.getDateFin().isBefore(request.getDateDebut())) {
            throw new BusinessValidationException("La date de fin doit être après la date de début");
        }

        // Calcul simple de la charge prévue (Jours calendaires * taux)
        long jours = ChronoUnit.DAYS.between(request.getDateDebut(), request.getDateFin()) + 1;
        Double charge = jours * (request.getTauxAffectation() / 100.0);

        Affectation affectation = Affectation.builder()
                .projet(projet)
                .collaborateur(collaborateur)
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .tauxAffectation(request.getTauxAffectation())
                .chargePrevue(charge)
                .roleDansProjet(request.getRoleDansProjet())
                .build();

        Affectation saved = affectationRepository.save(affectation);
        
        // Déclencher la détection d'anomalies
        anomalieService.detecterSurcharge(collaborateur.getId());
        
        return mapToResponseDTO(saved);
    }

    @Override
    public List<AffectationResponseDTO> getAffectationsParProjet(Long projetId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));
        
        return affectationRepository.findByProjet(projet).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void supprimerAffectation(Long id) {
        if (!affectationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Affectation non trouvée");
        }
        affectationRepository.deleteById(id);
    }

    private AffectationResponseDTO mapToResponseDTO(Affectation aff) {
        return AffectationResponseDTO.builder()
                .id(aff.getId())
                .projetId(aff.getProjet().getId())
                .projetNom(aff.getProjet().getNom())
                .collaborateurId(aff.getCollaborateur().getId())
                .collaborateurNomComplet(aff.getCollaborateur().getNom() + " " + aff.getCollaborateur().getPrenom())
                .dateDebut(aff.getDateDebut())
                .dateFin(aff.getDateFin())
                .tauxAffectation(aff.getTauxAffectation())
                .chargePrevue(aff.getChargePrevue())
                .roleDansProjet(aff.getRoleDansProjet())
                .build();
    }
}
