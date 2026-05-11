package com.backend.backend_pfe.DTO.request;


import com.backend.backend_pfe.enums.StatutProjet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetRequestDTO {

    @NotBlank(message = "Le nom du projet est obligatoire")
    private String nom;

    private String description;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    private StatutProjet statut;

    @NotNull(message = "L'identifiant du chef de projet est obligatoire")
    private Long chefProjetId;
}