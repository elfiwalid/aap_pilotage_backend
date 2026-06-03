package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.StatutProjet;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour la vue Resource Manager — un projet avec son équipe.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RmProjetDTO {

    private Long id;
    private String nom;
    private String description;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private StatutProjet statut;
    private String chefProjetNomComplet;
    /** Avancement basé sur le temps écoulé (0-100%) */
    private int avancement;
    /** Membres de l'équipe avec leur rôle et taux */
    private List<MembreEquipeDTO> equipe;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MembreEquipeDTO {
        private Long id;
        private String nom;
        private String prenom;
        private String role;
        private double tauxAffectation;
    }
}
