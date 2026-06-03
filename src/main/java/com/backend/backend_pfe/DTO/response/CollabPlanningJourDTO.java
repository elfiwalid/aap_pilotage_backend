package com.backend.backend_pfe.DTO.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Représente le planning d'une journée pour un collaborateur :
 * la liste des projets actifs ce jour avec leur taux d'affectation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabPlanningJourDTO {

    private LocalDate date;
    private List<SlotDTO> slots;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SlotDTO {
        private Long projetId;
        private String projet;
        private String couleur;
        private double alloc;
    }
}
