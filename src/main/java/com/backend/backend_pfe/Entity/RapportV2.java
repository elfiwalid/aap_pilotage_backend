package com.backend.backend_pfe.Entity;

import com.backend.backend_pfe.enums.TypeRapport;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rapports_v2")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportV2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    @Enumerated(EnumType.STRING)
    private TypeRapport typeRapport;

    private LocalDate periodeDebut;

    private LocalDate periodeFin;

    private LocalDateTime dateGeneration;

    @ManyToOne
    @JoinColumn(name = "prevision_id")
    private Prevision prevision;

    @ManyToOne
    @JoinColumn(name = "projet_id")
    private Projet projet;
}