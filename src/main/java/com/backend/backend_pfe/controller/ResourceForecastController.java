package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.request.ResourceForecastRequestDTO;
import com.backend.backend_pfe.DTO.response.ResourceForecastResponseDTO;
import com.backend.backend_pfe.service.ResourceForecastService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
public class ResourceForecastController {

    private final ResourceForecastService resourceForecastService;

    @PostMapping("/resource-forecast")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<ResourceForecastResponseDTO> forecast(
            @Valid @RequestBody ResourceForecastRequestDTO request) {
        return ResponseEntity.ok(resourceForecastService.forecast(request));
    }
}
