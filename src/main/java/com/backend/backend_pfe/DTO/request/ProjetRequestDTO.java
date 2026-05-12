package com.backend.backend_pfe.DTO.request;


import com.backend.backend_pfe.enums.StatutProjet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetRequestDTO {

    @NotBlank(message = "Le nom du projet est obligatoire")
    @Size(max = 255, message = "Le nom ne doit pas dépasser 255 caractères")
    private String nom;

    @Size(max = 1500, message = "La description ne doit pas dépasser 1500 caractères")
    private String description;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    private StatutProjet statut; // nullable — défaut PLANIFIE géré côté service
}