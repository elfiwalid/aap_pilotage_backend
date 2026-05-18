package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.Notification;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.enums.StatutNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByDestinataire(User destinataire);

    List<Notification> findByDestinataireAndStatut(User destinataire, StatutNotification statut);

    List<Notification> findByDestinataireOrderByDateCreationDesc(User destinataire);

    List<Notification> findByExpediteur(User expediteur);
}