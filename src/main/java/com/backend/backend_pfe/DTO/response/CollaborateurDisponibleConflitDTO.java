package com.backend.backend_pfe.DTO.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaborateurDisponibleConflitDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String poste;
    private String matricule;
    private double tauxUtilisation;
    private double tauxApresSimulation;
    private double disponibilite;
    private double joursDisponibles;
}
