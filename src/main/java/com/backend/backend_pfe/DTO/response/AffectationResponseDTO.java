package com.backend.backend_pfe.DTO.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationResponseDTO {
    private Long id;
    private Long projetId;
    private String projetNom;
    private Long collaborateurId;
    private String collaborateurNomComplet;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Double tauxAffectation;
    private Double chargePrevue;
    private String roleDansProjet;
}
