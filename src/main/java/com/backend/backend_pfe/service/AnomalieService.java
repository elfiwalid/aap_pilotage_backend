package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.AnomalieResponseDTO;
import com.backend.backend_pfe.enums.StatutAnomalie;
import com.backend.backend_pfe.enums.TypeAnomalie;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface AnomalieService {
    List<AnomalieResponseDTO> getAnomalies(Authentication authentication, TypeAnomalie typeFilter, StatutAnomalie statutFilter);
    void resoudreAnomalie(Long anomalieId, Authentication authentication);
}
