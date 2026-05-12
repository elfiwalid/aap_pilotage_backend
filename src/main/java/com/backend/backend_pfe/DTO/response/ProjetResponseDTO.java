package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.StatutProjet;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateCreation;
}