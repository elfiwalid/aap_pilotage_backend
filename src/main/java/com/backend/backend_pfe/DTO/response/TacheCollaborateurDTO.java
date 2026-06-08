package com.backend.backend_pfe.DTO.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TacheCollaborateurDTO {

    private Long id;
    private Long projetId;
    private String projetNom;
    private Long collaborateurId;
    private String collaborateurNomComplet;
    private String matricule;
    private String tache;
    private LocalDate dateTache;
    private int ordreJour;
    private LocalDate dateDebutV2;
    private LocalDate dateFinV2;
}
