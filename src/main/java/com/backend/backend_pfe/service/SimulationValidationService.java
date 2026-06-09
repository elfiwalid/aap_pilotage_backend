package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.ConversationSimulationDTO;
import org.springframework.security.core.Authentication;

public interface SimulationValidationService {

    ConversationSimulationDTO acceptSimulation(Long simulationId, String commentaire, Authentication authentication);

    ConversationSimulationDTO rejectSimulation(Long simulationId, String commentaire, Authentication authentication);
}
