package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.ConversationSimulationDTO;
import com.backend.backend_pfe.DTO.response.MessageConversationDTO;
import com.backend.backend_pfe.DTO.response.SimulationDecisionDTO;
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

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private final ConversationSimulationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageConversationRepository messageRepository;
    private final SimulationDecisionRepository decisionRepository;
    private final SimulationWhatIfRepository simulationRepository;
    private final ScenarioWhatIfRepository scenarioRepository;
    private final AffectationRepository affectationRepository;
    private final UserRepository userRepository;

    @Override
    public ConversationSimulationDTO createConversationFromSimulation(Long simulationId, Authentication authentication) {
        User currentUser = resolveUser(authentication);
        SimulationWhatIf simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation introuvable"));
        ScenarioWhatIf scenario = getScenario(simulation);

        if (simulation.getResultat() != ResultatSimulationWhatIf.POSITIF) {
            throw new BusinessValidationException("Impossible de creer une conversation pour une simulation impossible");
        }

        if (!Boolean.TRUE.equals(scenario.getSimulationGlobaleConflit())) {
            throw new BusinessValidationException("Seules les simulations globales de conflit peuvent creer une conversation");
        }

        if (simulation.getResourceManager() == null
                || !simulation.getResourceManager().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Seul le Resource Manager createur peut creer la conversation");
        }

        Optional<ConversationSimulation> existing = conversationRepository.findBySimulation(simulation);
        if (existing.isPresent()) {
            return toConversationDTO(existing.get());
        }

        ConversationSimulation conversation = ConversationSimulation.builder()
                .simulation(simulation)
                .createdBy(currentUser)
                .status(ConversationSimulationStatus.ACTIVE)
                .dateCreation(LocalDateTime.now())
                .build();
        conversation = conversationRepository.save(conversation);

        addParticipant(conversation, currentUser, null, false);

        for (Projet projet : getProjetsConflit(scenario)) {
            User chef = projet.getChefProjet();
            if (chef == null) continue;
            addParticipant(conversation, chef, projet, true);
            decisionRepository.save(SimulationDecision.builder()
                    .conversation(conversation)
                    .simulation(simulation)
                    .chefProjet(chef)
                    .projet(projet)
                    .status(SimulationDecisionStatus.PENDING)
                    .build());
        }

        addSystemMessage(conversation.getId(), buildInitialSystemMessage(simulation, scenario));
        return toConversationDTO(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSimulationDTO> getMyConversations(Authentication authentication) {
        User user = resolveUser(authentication);
        return conversationRepository.findByParticipant(user).stream()
                .map(this::toConversationDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationSimulationDTO getConversationById(Long conversationId, Authentication authentication) {
        User user = resolveUser(authentication);
        ConversationSimulation conversation = getConversation(conversationId);
        verifyParticipant(conversation, user);
        return toConversationDTO(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageConversationDTO> getMessages(Long conversationId, Authentication authentication) {
        User user = resolveUser(authentication);
        ConversationSimulation conversation = getConversation(conversationId);
        verifyParticipant(conversation, user);
        return messageRepository.findByConversationOrderByDateEnvoiAsc(conversation).stream()
                .map(this::toMessageDTO)
                .toList();
    }

    @Override
    public MessageConversationDTO sendMessage(Long conversationId, String content, Authentication authentication) {
        User user = resolveUser(authentication);
        ConversationSimulation conversation = getConversation(conversationId);
        verifyParticipant(conversation, user);

        if (content == null || content.isBlank()) {
            throw new BusinessValidationException("Le message ne peut pas etre vide");
        }

        MessageConversation message = MessageConversation.builder()
                .conversation(conversation)
                .auteur(user)
                .type(MessageConversationType.USER)
                .contenu(content.trim())
                .dateEnvoi(LocalDateTime.now())
                .build();
        return toMessageDTO(messageRepository.save(message));
    }

    @Override
    public MessageConversationDTO addSystemMessage(Long conversationId, String content) {
        ConversationSimulation conversation = getConversation(conversationId);
        MessageConversation message = MessageConversation.builder()
                .conversation(conversation)
                .auteur(null)
                .type(MessageConversationType.SYSTEM)
                .contenu(content)
                .dateEnvoi(LocalDateTime.now())
                .build();
        return toMessageDTO(messageRepository.save(message));
    }

    private void addParticipant(ConversationSimulation conversation, User user, Projet projet, boolean chefProjetConcerne) {
        if (participantRepository.existsByConversationAndUser(conversation, user)) {
            return;
        }
        participantRepository.save(ConversationParticipant.builder()
                .conversation(conversation)
                .user(user)
                .projet(projet)
                .chefProjetConcerne(chefProjetConcerne)
                .build());
    }

    private String buildInitialSystemMessage(SimulationWhatIf simulation, ScenarioWhatIf scenario) {
        String projets = String.join(", ", getProjetsConflit(scenario).stream().map(Projet::getNom).toList());
        String chefs = String.join(", ", getProjetsConflit(scenario).stream()
                .map(Projet::getChefProjet)
                .filter(Objects::nonNull)
                .map(this::buildFullName)
                .distinct()
                .toList());

        return String.format("""
                Proposition de resolution de conflit :
                Le collaborateur %s est en conflit sur la periode du %s au %s entre les projets %s.
                Le Resource Manager propose de le remplacer par %s.
                Chefs de projet concernes : %s.
                Simulation : %s.
                Impact estime :
                - %s : charge avant %.1f%%, charge apres %.1f%%.
                - %s : charge avant %.1f%%, charge apres %.1f%%.
                Cette proposition n'est pas encore appliquee. Elle sera appliquee uniquement sur le projet du chef de projet qui accepte.
                """,
                buildFullName(scenario.getCollaborateurSource()),
                scenario.getDateDebut(),
                scenario.getDateFin(),
                projets,
                buildFullName(scenario.getCollaborateurCible()),
                chefs,
                simulation.getResultat(),
                buildFullName(scenario.getCollaborateurSource()),
                value(scenario.getTauxSourceAvant()),
                value(scenario.getTauxSourceApres()),
                buildFullName(scenario.getCollaborateurCible()),
                value(scenario.getTauxCibleAvant()),
                value(scenario.getTauxCibleApres())
        );
    }

    private List<Projet> getProjetsConflit(ScenarioWhatIf scenario) {
        Map<Long, Projet> projets = new LinkedHashMap<>();
        affectationRepository.findAffectationsChevauchantes(
                        scenario.getCollaborateurSource().getId(),
                        scenario.getDateDebut(),
                        scenario.getDateFin())
                .stream()
                .filter(aff -> aff.getProjet() != null)
                .forEach(aff -> projets.putIfAbsent(aff.getProjet().getId(), aff.getProjet()));
        return projets.values().stream().toList();
    }

    private ConversationSimulationDTO toConversationDTO(ConversationSimulation conversation) {
        ScenarioWhatIf scenario = getScenario(conversation.getSimulation());
        List<Projet> projets = getProjetsConflit(scenario);

        return ConversationSimulationDTO.builder()
                .id(conversation.getId())
                .simulationId(conversation.getSimulation().getId())
                .status(conversation.getStatus())
                .dateCreation(conversation.getDateCreation())
                .createdByNomComplet(buildFullName(conversation.getCreatedBy()))
                .collaborateurSource(buildFullName(scenario.getCollaborateurSource()))
                .collaborateurCible(buildFullName(scenario.getCollaborateurCible()))
                .dateDebut(scenario.getDateDebut())
                .dateFin(scenario.getDateFin())
                .projetsConflit(projets.stream().map(Projet::getNom).toList())
                .resultat(conversation.getSimulation().getResultat())
                .statutSimulation(conversation.getSimulation().getStatut())
                .tauxSourceAvant(scenario.getTauxSourceAvant())
                .tauxSourceApres(scenario.getTauxSourceApres())
                .tauxCibleAvant(scenario.getTauxCibleAvant())
                .tauxCibleApres(scenario.getTauxCibleApres())
                .participants(participantRepository.findByConversation(conversation).stream()
                        .map(this::toParticipantDTO)
                        .toList())
                .decisions(decisionRepository.findByConversation(conversation).stream()
                        .map(this::toDecisionDTO)
                        .toList())
                .build();
    }

    private ConversationSimulationDTO.ParticipantDTO toParticipantDTO(ConversationParticipant participant) {
        return ConversationSimulationDTO.ParticipantDTO.builder()
                .userId(participant.getUser().getId())
                .nomComplet(buildFullName(participant.getUser()))
                .role(participant.getUser().getRole().name())
                .projetId(participant.getProjet() != null ? participant.getProjet().getId() : null)
                .projetNom(participant.getProjet() != null ? participant.getProjet().getNom() : null)
                .chefProjetConcerne(participant.isChefProjetConcerne())
                .build();
    }

    private SimulationDecisionDTO toDecisionDTO(SimulationDecision decision) {
        return SimulationDecisionDTO.builder()
                .id(decision.getId())
                .chefProjetId(decision.getChefProjet().getId())
                .chefProjetNomComplet(buildFullName(decision.getChefProjet()))
                .projetId(decision.getProjet().getId())
                .projetNom(decision.getProjet().getNom())
                .status(decision.getStatus())
                .commentaire(decision.getCommentaire())
                .dateDecision(decision.getDateDecision())
                .build();
    }

    private MessageConversationDTO toMessageDTO(MessageConversation message) {
        return MessageConversationDTO.builder()
                .id(message.getId())
                .auteurId(message.getAuteur() != null ? message.getAuteur().getId() : null)
                .auteurNomComplet(message.getAuteur() != null ? buildFullName(message.getAuteur()) : "Systeme")
                .type(message.getType())
                .contenu(message.getContenu())
                .dateEnvoi(message.getDateEnvoi())
                .build();
    }

    private ConversationSimulation getConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable"));
    }

    private ScenarioWhatIf getScenario(SimulationWhatIf simulation) {
        return scenarioRepository.findBySimulationId(simulation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scenario introuvable"));
    }

    private void verifyParticipant(ConversationSimulation conversation, User user) {
        if (!participantRepository.existsByConversationAndUser(conversation, user)) {
            throw new AccessDeniedException("Acces refuse a cette conversation");
        }
    }

    private User resolveUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private String buildFullName(User user) {
        return user == null ? null : user.getPrenom() + " " + user.getNom();
    }

    private double value(Double value) {
        return value != null ? value : 0.0;
    }
}
