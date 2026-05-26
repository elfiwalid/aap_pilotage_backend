package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.TypePrevision;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrevisionResponseDTO {

    private Long id;

    private String nomFichier;

    private TypePrevision typePrevision;

    private LocalDate periodeDebut;

    private LocalDate periodeFin;

    private LocalDateTime dateImport;

    private Boolean active;

    private String importeParNomComplet;

    private Long projetId;

    private String projetNom;
}
