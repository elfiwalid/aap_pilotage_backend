package com.backend.backend_pfe.DTO.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PmRapportMensuelDTO {

    private int annee;
    private int mois;
    private String libellePeriode;
    private int nombreTotalAnomalies;
    private int nombreConflits;
    private int nombreSurcharges;
    private int nombreSousCharges;
    private int nombreNonStaffes;
    private int nombreCollaborateursConcernes;
    private int nombreProjetsConcernes;
    private List<String> projetsConcernes;
    private Double allocationMoyenne;
    private String statut;
    private List<PmRapportAnomalieDTO> anomalies;
}
