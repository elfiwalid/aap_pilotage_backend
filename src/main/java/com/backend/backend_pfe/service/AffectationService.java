package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.AffectationRequestDTO;
import com.backend.backend_pfe.DTO.response.AffectationResponseDTO;

import java.util.List;

public interface AffectationService {
    AffectationResponseDTO creerAffectation(AffectationRequestDTO request);
    List<AffectationResponseDTO> getAffectationsParProjet(Long projetId);
    void supprimerAffectation(Long id);
}
