package com.backend.backend_pfe.DTO.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PmRapportAnomalieDTO {

    private Long idAnomalie;
    private String collaborateur;
    private String projetsConcernes;
    private String typeAnomalie;
    private String statutAnomalie;
    private int mois;
    private int annee;
    private int capaciteMensuelle;
    private int joursDemandes;
    private double tauxCharge;
    private String messageExplicatif;
}
