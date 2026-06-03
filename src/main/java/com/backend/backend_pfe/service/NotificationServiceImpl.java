package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.NotificationResponseDTO;
import com.backend.backend_pfe.Entity.Anomalie;
import com.backend.backend_pfe.Entity.Notification;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.NotificationRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.StatutNotification;
import com.backend.backend_pfe.enums.TypeNotification;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implémentation du service de notifications.
 *
 * SOLID — SRP : centralise la logique de création/lecture des notifications.
 * La création est résiliente (try-catch) pour ne jamais bloquer le flux appelant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public List<NotificationResponseDTO> getMesNotifications(Authentication authentication) {
        User user = resolveUser(authentication);
        return notificationRepository.findByDestinataireOrderByDateCreationDesc(user).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public long countNonLues(Authentication authentication) {
        User user = resolveUser(authentication);
        return notificationRepository.countByDestinataireAndStatut(user, StatutNotification.NON_LUE);
    }

    @Override
    @Transactional
    public void marquerCommeLue(Long id, Authentication authentication) {
        User user = resolveUser(authentication);
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));
        verifyOwnership(notif, user);
        notif.setStatut(StatutNotification.LUE);
        notificationRepository.save(notif);
    }

    @Override
    @Transactional
    public void marquerToutesCommeLues(Authentication authentication) {
        User user = resolveUser(authentication);
        List<Notification> nonLues = notificationRepository
                .findByDestinataireAndStatut(user, StatutNotification.NON_LUE);
        nonLues.forEach(n -> n.setStatut(StatutNotification.LUE));
        notificationRepository.saveAll(nonLues);
    }

    @Override
    @Transactional
    public void supprimer(Long id, Authentication authentication) {
        User user = resolveUser(authentication);
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));
        verifyOwnership(notif, user);
        notificationRepository.delete(notif);
    }

    // ─── Création ───

    @Override
    @Transactional
    public void creerNotification(User destinataire, User expediteur, TypeNotification type,
                                  String titre, String message, Anomalie anomalie) {
        if (destinataire == null) return;
        try {
            Notification notif = Notification.builder()
                    .titre(titre)
                    .message(message)
                    .type(type)
                    .statut(StatutNotification.NON_LUE)
                    .dateCreation(LocalDateTime.now())
                    .destinataire(destinataire)
                    .expediteur(expediteur)
                    .anomalie(anomalie)
                    .build();
            notificationRepository.save(notif);
        } catch (Exception e) {
            log.error("Échec de création de notification pour {}: {}",
                    destinataire.getEmail(), e.getMessage());
        }
    }

    @Override
    public void notifierAnomalie(Anomalie anomalie) {
        if (anomalie == null || anomalie.getProjet() == null) return;
        User chef = anomalie.getProjet().getChefProjet();
        if (chef == null) return;

        String collab = anomalie.getCollaborateur() != null
                ? anomalie.getCollaborateur().getPrenom() + " " + anomalie.getCollaborateur().getNom()
                : "Un collaborateur";

        creerNotification(
                chef, null, TypeNotification.ANOMALIE,
                anomalie.getTitre() != null ? anomalie.getTitre() : "Anomalie détectée",
                String.format("%s — Projet : %s. %s", collab,
                        anomalie.getProjet().getNom(),
                        anomalie.getDescription() != null ? anomalie.getDescription() : ""),
                anomalie);
    }

    @Override
    public void notifierAffectation(User collaborateur, Projet projet, double taux) {
        if (collaborateur == null || projet == null) return;
        User chef = projet.getChefProjet();
        creerNotification(
                collaborateur, chef, TypeNotification.AFFECTATION,
                "Nouvelle affectation — " + projet.getNom(),
                String.format("Vous avez été affecté au projet « %s » à hauteur de %.0f%%.",
                        projet.getNom(), taux),
                null);
    }

    // ─── Helpers ───

    private NotificationResponseDTO mapToDTO(Notification n) {
        String expediteur = n.getExpediteur() != null
                ? n.getExpediteur().getPrenom() + " " + n.getExpediteur().getNom()
                : "Système";
        return NotificationResponseDTO.builder()
                .id(n.getId())
                .titre(n.getTitre())
                .message(n.getMessage())
                .type(n.getType() != null ? n.getType().name() : TypeNotification.SYSTEME.name())
                .dateCreation(n.getDateCreation() != null ? n.getDateCreation().toString() : null)
                .lu(n.getStatut() == StatutNotification.LUE)
                .expediteurNomComplet(expediteur)
                .build();
    }

    private void verifyOwnership(Notification notif, User user) {
        if (notif.getDestinataire() == null
                || !notif.getDestinataire().getId().equals(user.getId())) {
            throw new AccessDeniedException("Accès refusé");
        }
    }

    private User resolveUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}
