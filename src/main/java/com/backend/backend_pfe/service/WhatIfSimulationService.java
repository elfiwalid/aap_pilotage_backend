package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.SimulationRemplacementRequestDTO;
import com.backend.backend_pfe.DTO.request.SimulationDepuisConflitRequestDTO;
import com.backend.backend_pfe.DTO.request.SimulationSousChargeRequestDTO;
import com.backend.backend_pfe.DTO.response.CollaborateurDisponibleConflitDTO;
import com.backend.backend_pfe.DTO.response.SimulationConflitContextDTO;
import com.backend.backend_pfe.DTO.response.SimulationRemplacementResponseDTO;
import com.backend.backend_pfe.DTO.response.SimulationSousChargeResponseDTO;

import java.util.List;

public interface WhatIfSimulationService {

    SimulationRemplacementResponseDTO simulerRemplacement(SimulationRemplacementRequestDTO request);
    SimulationSousChargeResponseDTO simulerSousCharge(SimulationSousChargeRequestDTO request);
    SimulationConflitContextDTO getConflitContext(Long conflitId);
    List<CollaborateurDisponibleConflitDTO> getCollaborateursDisponiblesPourConflit(Long conflitId);
    SimulationRemplacementResponseDTO simulerDepuisConflit(SimulationDepuisConflitRequestDTO request);

    void validerSimulation(Long simulationId);

    void annulerSimulation(Long simulationId);
}
