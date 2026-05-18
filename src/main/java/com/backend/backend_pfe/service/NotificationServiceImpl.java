package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.NotificationResponseDTO;
import com.backend.backend_pfe.Entity.Notification;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.NotificationRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.StatutNotification;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public void envoyerNotification(Long userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Notification notification = Notification.builder()
                .message(message)
                .dateCreation(LocalDateTime.now())
                .statut(StatutNotification.NON_LUE)
                .destinataire(user)
                .build();

        notificationRepository.save(notification);
    }

    @Override
    public List<NotificationResponseDTO> getMesNotifications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        return notificationRepository.findByDestinataireOrderByDateCreationDesc(user).stream()
                .map(n -> NotificationResponseDTO.builder()
                        .id(n.getId())
                        .message(n.getMessage())
                        .dateNotification(n.getDateCreation())
                        .lu(n.getStatut() == StatutNotification.LUE)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void marquerCommeLue(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée"));
        notification.setStatut(StatutNotification.LUE);
        notificationRepository.save(notification);
    }
}
