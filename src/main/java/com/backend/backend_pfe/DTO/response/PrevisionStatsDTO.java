package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.TypePrevision;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrevisionStatsDTO {

    private Integer nombreCollaborateurs;

    private Integer nombreMois;

    private TypePrevision typePrevision;

    private LocalDateTime dateImport;
}
