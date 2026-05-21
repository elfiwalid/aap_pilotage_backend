package com.backend.backend_pfe.Entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "affectations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Affectation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private Double tauxAffectation;

    private Double chargePrevue;
    private Double tjm;
    private Integer nombreJours;

    private String roleDansProjet;

    @ManyToOne
    @JoinColumn(name = "projet_id")
    private Projet projet;

    @ManyToOne
    @JoinColumn(name = "collaborateur_id")
    private User collaborateur;
}