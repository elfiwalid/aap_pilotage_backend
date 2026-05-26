package com.backend.backend_pfe.Entity;


import com.backend.backend_pfe.enums.TypePrevision;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "previsions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String nomFichier;

    @Enumerated(EnumType.STRING)
    private TypePrevision typePrevision;

    private LocalDate periodeDebut;

    private LocalDate periodeFin;

    private LocalDateTime dateImport;

    private Boolean active;

    @Column(columnDefinition = "BYTEA")
    private byte[] fichierData;

    @ManyToOne
    @JoinColumn(name = "importe_par_id")
    private User importePar;

    @ManyToOne
    @JoinColumn(name = "projet_id")
    private Projet projet;
}