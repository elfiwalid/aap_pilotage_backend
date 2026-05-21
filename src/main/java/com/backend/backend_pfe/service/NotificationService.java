package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.NotificationResponseDTO;
import java.util.List;

public interface NotificationService {
    void envoyerNotification(Long userId, String message);
    List<NotificationResponseDTO> getMesNotifications(String email);
    void marquerCommeLue(Long id);
}
