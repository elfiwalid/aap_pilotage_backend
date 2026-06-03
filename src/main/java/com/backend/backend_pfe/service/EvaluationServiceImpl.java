package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.EvaluationRequestDTO;
import com.backend.backend_pfe.DTO.response.EvaluationResponseDTO;
import com.backend.backend_pfe.Entity.Evaluation;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.EvaluationRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.Role;
import com.backend.backend_pfe.exception.BusinessValidationException;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of the EvaluationService interface.
 *
 * SOLID — SRP: handles only evaluation business logic.
 * SOLID — OCP: new evaluation criteria can be added by extending
 *   the entity and DTO without modifying existing logic.
 */
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public EvaluationResponseDTO evaluerCollaborateur(Authentication authentication, EvaluationRequestDTO request) {
        User evaluateur = findUserByAuth(authentication);

        // Verify the evaluator is a Chef de Projet
        if (evaluateur.getRole() != Role.CHEF_PROJET) {
            throw new BusinessValidationException("Seul un Chef de Projet peut évaluer un collaborateur.");
        }

        // Verify the collaborateur exists and has the COLLABORATEUR role
        User collaborateur = userRepository.findById(request.getCollaborateurId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Collaborateur introuvable avec l'ID : " + request.getCollaborateurId()));

        if (collaborateur.getRole() != Role.COLLABORATEUR) {
            throw new BusinessValidationException("L'utilisateur sélectionné n'est pas un collaborateur.");
        }

        // Check if an evaluation already exists for this combination → update it
        Optional<Evaluation> existing = evaluationRepository
                .findByCollaborateurIdAndEvaluateurIdAndAnneeAndMois(
                        request.getCollaborateurId(),
                        evaluateur.getId(),
                        request.getAnnee(),
                        request.getMois());

        Evaluation evaluation;
        if (existing.isPresent()) {
            evaluation = existing.get();
            evaluation.setQualiteTravail(request.getQualiteTravail());
            evaluation.setRespectDelais(request.getRespectDelais());
            evaluation.setTravailEquipe(request.getTravailEquipe());
            evaluation.setCommunication(request.getCommunication());
            evaluation.setCommentaire(request.getCommentaire());
        } else {
            evaluation = Evaluation.builder()
                    .collaborateur(collaborateur)
                    .evaluateur(evaluateur)
                    .mois(request.getMois())
                    .annee(request.getAnnee())
                    .qualiteTravail(request.getQualiteTravail())
                    .respectDelais(request.getRespectDelais())
                    .travailEquipe(request.getTravailEquipe())
                    .communication(request.getCommunication())
                    .commentaire(request.getCommentaire())
                    .build();
        }

        Evaluation saved = evaluationRepository.save(evaluation);
        return toDTO(saved);
    }

    @Override
    public List<EvaluationResponseDTO> getMesEvaluations(Authentication authentication) {
        User user = findUserByAuth(authentication);
        return evaluationRepository.findByCollaborateurIdOrderByAnneeDescMoisDesc(user.getId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EvaluationResponseDTO> getEvaluationsParChef(Authentication authentication) {
        User user = findUserByAuth(authentication);
        return evaluationRepository.findByEvaluateurIdOrderByAnneeDescMoisDesc(user.getId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EvaluationResponseDTO> getEvaluationsCollaborateur(Long collaborateurId) {
        if (!userRepository.existsById(collaborateurId)) {
            throw new ResourceNotFoundException("Collaborateur introuvable avec l'ID : " + collaborateurId);
        }
        return evaluationRepository.findByCollaborateurIdOrderByAnneeDescMoisDesc(collaborateurId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────

    private User findUserByAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable : " + email));
    }

    private EvaluationResponseDTO toDTO(Evaluation evaluation) {
        double moyenne = (evaluation.getQualiteTravail()
                + evaluation.getRespectDelais()
                + evaluation.getTravailEquipe()
                + evaluation.getCommunication()) / 4.0;

        // Round to 1 decimal place
        moyenne = Math.round(moyenne * 10.0) / 10.0;

        return EvaluationResponseDTO.builder()
                .id(evaluation.getId())
                .collaborateurId(evaluation.getCollaborateur().getId())
                .collaborateurNom(evaluation.getCollaborateur().getNom())
                .collaborateurPrenom(evaluation.getCollaborateur().getPrenom())
                .evaluateurId(evaluation.getEvaluateur().getId())
                .evaluateurNom(evaluation.getEvaluateur().getNom())
                .evaluateurPrenom(evaluation.getEvaluateur().getPrenom())
                .mois(evaluation.getMois())
                .annee(evaluation.getAnnee())
                .qualiteTravail(evaluation.getQualiteTravail())
                .respectDelais(evaluation.getRespectDelais())
                .travailEquipe(evaluation.getTravailEquipe())
                .communication(evaluation.getCommunication())
                .moyenneGenerale(moyenne)
                .commentaire(evaluation.getCommentaire())
                .dateCreation(evaluation.getDateCreation())
                .build();
    }
}
