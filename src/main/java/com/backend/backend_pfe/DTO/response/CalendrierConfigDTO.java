package com.backend.backend_pfe.DTO.response;

import lombok.*;

import java.util.List;

/**
 * DTO pour la configuration du calendrier (jours ouvrables + jours fériés).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendrierConfigDTO {

    private String pays;
    private int annee;
    private List<MoisOuvrableDTO> mois;
    private List<JourFerieDTO> joursFeries;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MoisOuvrableDTO {
        private int mois;          // 1-12
        private String label;      // "Janvier 2026"
        private int joursTotal;    // jours dans le mois
        private int weekends;      // nombre de jours weekend
        private int joursFeries;   // nombre de jours fériés (hors weekend)
        private int joursOuvrablesAuto; // total - weekends - fériés
        private Integer joursOuvrablesManuel; // valeur modifiée par le RM (null = pas modifié)
        private boolean valide;    // validé par le RM
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JourFerieDTO {
        private String date;       // ISO yyyy-MM-dd
        private String nom;
        private boolean actif;     // true = compté comme férié
    }
}
