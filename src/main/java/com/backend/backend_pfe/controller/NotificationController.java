package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.response.NotificationResponseDTO;
import com.backend.backend_pfe.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getMesNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getMesNotifications(authentication.getName()));
    }

    @PutMapping("/{id}/lu")
    public ResponseEntity<Void> marquerCommeLue(@PathVariable Long id) {
        notificationService.marquerCommeLue(id);
        return ResponseEntity.ok().build();
    }
}
