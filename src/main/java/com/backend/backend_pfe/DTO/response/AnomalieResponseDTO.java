package com.backend.backend_pfe.DTO.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalieResponseDTO {

    private Long id;

    private String titre;

    private String description;

    private String typeAnomalie;

    private String statut;

    private String dateDetection;

    private boolean resolu;

    private Long projetId;

    private String projetNom;

    private Long collaborateurId;

    private String collaborateurNomComplet;
}
