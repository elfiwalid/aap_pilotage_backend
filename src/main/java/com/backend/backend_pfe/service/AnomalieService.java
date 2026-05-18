package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.AnomalieResponseDTO;
import java.util.List;

public interface AnomalieService {
    void detecterSurcharge(Long collaborateurId);
    List<AnomalieResponseDTO> getAllAnomalies();
    void resoudreAnomalie(Long id);
}
