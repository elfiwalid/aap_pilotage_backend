package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.ConversationSimulationDTO;
import com.backend.backend_pfe.DTO.response.MessageConversationDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ConversationService {

    ConversationSimulationDTO createConversationFromSimulation(Long simulationId, Authentication authentication);

    List<ConversationSimulationDTO> getMyConversations(Authentication authentication);

    ConversationSimulationDTO getConversationById(Long conversationId, Authentication authentication);

    List<MessageConversationDTO> getMessages(Long conversationId, Authentication authentication);

    MessageConversationDTO sendMessage(Long conversationId, String content, Authentication authentication);

    MessageConversationDTO addSystemMessage(Long conversationId, String content);
}
