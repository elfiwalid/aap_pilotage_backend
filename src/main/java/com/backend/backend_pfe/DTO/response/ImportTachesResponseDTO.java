package com.backend.backend_pfe.DTO.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportTachesResponseDTO {

    private int lignesTraitees;
    private int tachesPlanifiees;
    private int collaborateursConcernes;

    @Builder.Default
    private List<TacheCollaborateurDTO> taches = new ArrayList<>();
}
