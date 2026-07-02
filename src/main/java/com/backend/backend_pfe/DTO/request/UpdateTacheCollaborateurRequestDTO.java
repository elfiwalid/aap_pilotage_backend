package com.backend.backend_pfe.DTO.request;

import com.backend.backend_pfe.enums.StatutTache;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTacheCollaborateurRequestDTO {

    @NotNull
    private StatutTache statut;

    @Min(0)
    @Max(100)
    private Integer pourcentageAvancement;
}
