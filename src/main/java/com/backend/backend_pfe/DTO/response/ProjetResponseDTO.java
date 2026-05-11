package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.StatutProjet;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetResponseDTO {

    private Long id;

    private String nom;

    private String description;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private StatutProjet statut;

    private Long chefProjetId;

    private String chefProjetNomComplet;
}