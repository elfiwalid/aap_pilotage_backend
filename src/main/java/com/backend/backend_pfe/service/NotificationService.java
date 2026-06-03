package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.NotificationResponseDTO;
import com.backend.backend_pfe.Entity.Anomalie;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.enums.TypeNotification;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Service de gestion des notifications, commun aux trois profils.
 */
public interface NotificationService {

    /** Notifications du destinataire connecté, triées par date décroissante. */
    List<NotificationResponseDTO> getMesNotifications(Authentication authentication);

    /** Nombre de notifications non lues du destinataire connecté. */
    long countNonLues(Authentication authentication);

    /** Marquer une notification comme lue (vérifie la propriété). */
    void marquerCommeLue(Long id, Authentication authentication);

    /** Marquer toutes les notifications du destinataire comme lues. */
    void marquerToutesCommeLues(Authentication authentication);

    /** Supprimer une notification (vérifie la propriété). */
    void supprimer(Long id, Authentication authentication);

    // ─── Création (appelée par d'autres services) ───

    /** Crée une notification générique pour un destinataire. */
    void creerNotification(User destinataire, User expediteur, TypeNotification type,
                           String titre, String message, Anomalie anomalie);

    /** Notifie un chef de projet qu'une anomalie a été détectée sur son projet. */
    void notifierAnomalie(Anomalie anomalie);

    /** Notifie un collaborateur qu'il a été affecté à un projet. */
    void notifierAffectation(User collaborateur, Projet projet, double taux);
}
