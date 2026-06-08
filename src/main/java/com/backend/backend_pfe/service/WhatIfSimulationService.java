package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.SimulationRemplacementRequestDTO;
import com.backend.backend_pfe.DTO.request.SimulationSousChargeRequestDTO;
import com.backend.backend_pfe.DTO.response.SimulationRemplacementResponseDTO;
import com.backend.backend_pfe.DTO.response.SimulationSousChargeResponseDTO;

public interface WhatIfSimulationService {

    SimulationRemplacementResponseDTO simulerRemplacement(SimulationRemplacementRequestDTO request);
    SimulationSousChargeResponseDTO simulerSousCharge(SimulationSousChargeRequestDTO request);

    void validerSimulation(Long simulationId);

    void annulerSimulation(Long simulationId);
}
