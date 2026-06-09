package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.PmRapportMensuelDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface PmRapportV2Service {

    List<PmRapportMensuelDTO> getRapportsMensuels(Authentication authentication);
}
