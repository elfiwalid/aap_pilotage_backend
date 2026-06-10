package com.backend.backend_pfe.DTO.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceForecastResponseDTO {

    private int currentResources;
    private int predictedResources;
    private int difference;
    private String riskLevel;
}
