package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.StatutProjet;
import lombok.*;

import java.time.LocalDate;

/**
 * Représente un projet du point de vue d'un collaborateur :
 * son rôle, son taux d'affectation, l'avancement (basé sur le temps), etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabProjetDTO {

    private Long id;
    private String nom;
    private String description;
    private String role;
    private Double tauxAffectation;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private StatutProjet statut;
    private String chefProjetNomComplet;
    private String couleur;
    /** Avancement basé sur le temps écoulé (0-100%) */
    private int avancement;
    /** Nombre de collaborateurs distincts sur le projet */
    private int tailleEquipe;
}
