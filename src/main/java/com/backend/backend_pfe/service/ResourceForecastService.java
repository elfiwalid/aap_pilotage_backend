package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.ResourceForecastRequestDTO;
import com.backend.backend_pfe.DTO.response.ResourceForecastResponseDTO;

public interface ResourceForecastService {
    ResourceForecastResponseDTO forecast(ResourceForecastRequestDTO request);
}
