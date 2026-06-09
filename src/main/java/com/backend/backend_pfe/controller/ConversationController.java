package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.request.CreateMessageConversationRequest;
import com.backend.backend_pfe.DTO.response.ConversationSimulationDTO;
import com.backend.backend_pfe.DTO.response.MessageConversationDTO;
import com.backend.backend_pfe.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping("/from-simulation/{simulationId}")
    public ResponseEntity<ConversationSimulationDTO> createFromSimulation(
            @PathVariable Long simulationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(conversationService.createConversationFromSimulation(simulationId, authentication));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ConversationSimulationDTO>> getMyConversations(Authentication authentication) {
        return ResponseEntity.ok(conversationService.getMyConversations(authentication));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationSimulationDTO> getConversation(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(conversationService.getConversationById(conversationId, authentication));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageConversationDTO>> getMessages(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(conversationService.getMessages(conversationId, authentication));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageConversationDTO> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody CreateMessageConversationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(conversationService.sendMessage(
                conversationId,
                request.getContenu(),
                authentication
        ));
    }
}
