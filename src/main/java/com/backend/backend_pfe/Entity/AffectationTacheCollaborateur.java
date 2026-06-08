package com.backend.backend_pfe.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "affectations_taches_collaborateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationTacheCollaborateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String tache;

    @Column(nullable = false)
    private LocalDate dateTache;

    @Column(nullable = false)
    private Integer ordreJour;

    @Column(nullable = false)
    private LocalDate dateDebutV2;

    @Column(nullable = false)
    private LocalDate dateFinV2;

    @Column(nullable = false)
    private LocalDateTime dateImport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collaborateur_id", nullable = false)
    private User collaborateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affectation_id", nullable = false)
    private Affectation affectation;
}
