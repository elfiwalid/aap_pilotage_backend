package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.ConversationSimulationDTO;
import com.backend.backend_pfe.Entity.*;
import com.backend.backend_pfe.Repository.*;
import com.backend.backend_pfe.enums.*;
import com.backend.backend_pfe.exception.BusinessValidationException;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SimulationValidationServiceImpl implements SimulationValidationService {

    private static final String DEFAULT_COUNTRY = "ma";

    private final SimulationWhatIfRepository simulationRepository;
    private final ScenarioWhatIfRepository scenarioRepository;
    private final ConversationSimulationRepository conversationRepository;
    private final SimulationDecisionRepository decisionRepository;
    private final AffectationRepository affectationRepository;
    private final UserRepository userRepository;
    private final ConversationService conversationService;
    private final AnomalieDetectionV2Service anomalieDetectionV2Service;

    @Override
    public ConversationSimulationDTO acceptSimulation(Long simulationId, String commentaire, Authentication authentication) {
        User chef = resolveUser(authentication);
        SimulationWhatIf simulation = getSimulation(simulationId);
        ScenarioWhatIf scenario = getScenario(simulation);
        ConversationSimulation conversation = getConversation(simulation);

        if (chef.getRole() != Role.CHEF_PROJET) {
            throw new AccessDeniedException("Seul un chef de projet concerne peut accepter la simulation");
        }

        if (simulation.getResultat() != ResultatSimulationWhatIf.POSITIF) {
            throw new BusinessValidationException("Impossible d'accepter une simulation impossible");
        }

        SimulationDecision decision = decisionRepository.findBySimulationAndChefProjet(simulation, chef)
                .orElseThrow(() -> new AccessDeniedException("Ce chef de projet n'est pas concerne par cette simulation"));

        if (decision.getStatus() != SimulationDecisionStatus.PENDING) {
            throw new BusinessValidationException("Une decision a deja ete enregistree pour ce chef de projet");
        }

        ensureTargetStillAvailableForProject(simulation, scenario, decision.getProjet());
        applyReplacementOnProject(scenario, decision.getProjet());

        decision.setStatus(SimulationDecisionStatus.ACCEPTED);
        decision.setCommentaire(commentaire);
        decision.setDateDecision(LocalDateTime.now());
        decisionRepository.save(decision);

        simulation.setStatut(StatutSimulationWhatIf.VALIDEE);
        simulation.setCommentaire("Simulation acceptee par " + buildFullName(chef)
                + " et appliquee sur le projet " + decision.getProjet().getNom() + ".");
        simulationRepository.save(simulation);

        closeConversationIfAllDecided(conversation, simulation);

        conversationService.addSystemMessage(conversation.getId(),
                "La proposition a ete acceptee par " + buildFullName(chef)
                        + ". Le remplacement a ete applique sur le projet "
                        + decision.getProjet().getNom() + ".");

        if (simulation.getAnomalie() != null) {
            anomalieDetectionV2Service.detecterAnomalies(
                    simulation.getAnomalie().getAnnee(),
                    simulation.getAnomalie().getMois(),
                    DEFAULT_COUNTRY
            );
        }

        return conversationService.getConversationById(conversation.getId(), authentication);
    }

    @Override
    public ConversationSimulationDTO rejectSimulation(Long simulationId, String commentaire, Authentication authentication) {
        User chef = resolveUser(authentication);
        SimulationWhatIf simulation = getSimulation(simulationId);
        ConversationSimulation conversation = getConversation(simulation);

        if (chef.getRole() != Role.CHEF_PROJET) {
            throw new AccessDeniedException("Seul un chef de projet concerne peut refuser la simulation");
        }

        SimulationDecision decision = decisionRepository.findBySimulationAndChefProjet(simulation, chef)
                .orElseThrow(() -> new AccessDeniedException("Ce chef de projet n'est pas concerne par cette simulation"));

        if (decision.getStatus() != SimulationDecisionStatus.PENDING) {
            throw new BusinessValidationException("Une decision a deja ete enregistree pour ce chef de projet");
        }

        decision.setStatus(SimulationDecisionStatus.REJECTED);
        decision.setCommentaire(commentaire);
        decision.setDateDecision(LocalDateTime.now());
        decisionRepository.save(decision);

        conversationService.addSystemMessage(conversation.getId(),
                "La proposition a ete refusee par " + buildFullName(chef)
                        + " pour le projet " + decision.getProjet().getNom() + ".");

        closeConversationIfAllDecided(conversation, simulation);

        return conversationService.getConversationById(conversation.getId(), authentication);
    }

    private void ensureTargetStillAvailableForProject(SimulationWhatIf simulation, ScenarioWhatIf scenario, Projet projet) {
        List<Long> acceptedProjectIds = decisionRepository.findBySimulation(simulation).stream()
                .filter(d -> d.getStatus() == SimulationDecisionStatus.ACCEPTED)
                .map(d -> d.getProjet().getId())
                .toList();

        List<Affectation> conflicts = affectationRepository.findAffectationsChevauchantes(
                scenario.getCollaborateurCible().getId(),
                scenario.getDateDebut(),
                scenario.getDateFin()
        ).stream()
                .filter(aff -> aff.getProjet() == null
                        || (!aff.getProjet().getId().equals(projet.getId())
                        && !acceptedProjectIds.contains(aff.getProjet().getId())))
                .toList();
        if (!conflicts.isEmpty()) {
            throw new BusinessValidationException("Le collaborateur cible n'est plus disponible sur la periode du conflit");
        }
    }

    private void closeConversationIfAllDecided(ConversationSimulation conversation, SimulationWhatIf simulation) {
        boolean hasPending = decisionRepository.findBySimulation(simulation).stream()
                .anyMatch(d -> d.getStatus() == SimulationDecisionStatus.PENDING);
        if (!hasPending) {
            conversation.setStatus(ConversationSimulationStatus.CLOSED);
            conversation.setDateCloture(LocalDateTime.now());
            conversationRepository.save(conversation);
        }
    }

    private void applyReplacementOnProject(ScenarioWhatIf scenario, Projet projet) {
        List<Affectation> sources = affectationRepository.findAffectationsProjetSurPeriode(
                scenario.getCollaborateurSource().getId(),
                projet.getId(),
                scenario.getDateDebut(),
                scenario.getDateFin()
        );

        if (sources.isEmpty()) {
            throw new BusinessValidationException("Affectation source introuvable pour le projet du chef sur la periode du conflit");
        }

        for (Affectation source : sources) {
            applyReplacementOnSourceSegment(scenario, projet, source);
        }
    }

    private void applyReplacementOnSourceSegment(ScenarioWhatIf scenario, Projet projet, Affectation source) {
        LocalDate oldStart = source.getDateDebut();
        LocalDate oldEnd = source.getDateFin();
        Double oldTaux = source.getTauxAffectation();
        String oldRole = source.getRoleDansProjet();
        LocalDate replacementStart = oldStart.isAfter(scenario.getDateDebut()) ? oldStart : scenario.getDateDebut();
        LocalDate replacementEnd = oldEnd.isBefore(scenario.getDateFin()) ? oldEnd : scenario.getDateFin();

        if (replacementStart.isAfter(replacementEnd)) {
            return;
        }

        if (oldStart.isBefore(scenario.getDateDebut()) && oldEnd.isAfter(scenario.getDateFin())) {
            source.setDateFin(scenario.getDateDebut().minusDays(1));
            affectationRepository.save(source);
            affectationRepository.save(Affectation.builder()
                    .collaborateur(scenario.getCollaborateurSource())
                    .projet(projet)
                    .dateDebut(scenario.getDateFin().plusDays(1))
                    .dateFin(oldEnd)
                    .tauxAffectation(oldTaux)
                    .roleDansProjet(oldRole)
                    .build());
        } else if (oldStart.isBefore(scenario.getDateDebut())) {
            source.setDateFin(scenario.getDateDebut().minusDays(1));
            affectationRepository.save(source);
        } else if (oldEnd.isAfter(scenario.getDateFin())) {
            source.setDateDebut(scenario.getDateFin().plusDays(1));
            affectationRepository.save(source);
        } else {
            affectationRepository.delete(source);
        }

        affectationRepository.save(Affectation.builder()
                .collaborateur(scenario.getCollaborateurCible())
                .projet(projet)
                .dateDebut(replacementStart)
                .dateFin(replacementEnd)
                .tauxAffectation(scenario.getTauxAffectation())
                .roleDansProjet(oldRole != null ? oldRole : "Collaborateur")
                .build());
    }

    private SimulationWhatIf getSimulation(Long simulationId) {
        return simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation introuvable"));
    }

    private ScenarioWhatIf getScenario(SimulationWhatIf simulation) {
        return scenarioRepository.findBySimulationId(simulation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scenario introuvable"));
    }

    private ConversationSimulation getConversation(SimulationWhatIf simulation) {
        return conversationRepository.findBySimulation(simulation)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable pour cette simulation"));
    }

    private User resolveUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private String buildFullName(User user) {
        return user == null ? null : user.getPrenom() + " " + user.getNom();
    }
}
