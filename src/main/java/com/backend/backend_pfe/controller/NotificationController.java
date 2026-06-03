package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.response.NotificationResponseDTO;
import com.backend.backend_pfe.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller des notifications, accessible aux trois profils
 * (chaque utilisateur ne voit que ses propres notifications).
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** GET /api/notifications — Liste des notifications de l'utilisateur connecté. */
    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getMesNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getMesNotifications(authentication));
    }

    /** GET /api/notifications/count — Nombre de notifications non lues. */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countNonLues(Authentication authentication) {
        return ResponseEntity.ok(Map.of("nonLues", notificationService.countNonLues(authentication)));
    }

    /** PUT /api/notifications/{id}/lu — Marquer une notification comme lue. */
    @PutMapping("/{id}/lu")
    public ResponseEntity<Void> marquerCommeLue(@PathVariable Long id, Authentication authentication) {
        notificationService.marquerCommeLue(id, authentication);
        return ResponseEntity.ok().build();
    }

    /** PUT /api/notifications/lu-toutes — Marquer toutes comme lues. */
    @PutMapping("/lu-toutes")
    public ResponseEntity<Void> marquerToutesCommeLues(Authentication authentication) {
        notificationService.marquerToutesCommeLues(authentication);
        return ResponseEntity.ok().build();
    }

    /** DELETE /api/notifications/{id} — Supprimer une notification. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id, Authentication authentication) {
        notificationService.supprimer(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
