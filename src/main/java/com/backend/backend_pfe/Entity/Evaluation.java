package com.backend.backend_pfe.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a monthly performance evaluation.
 *
 * A Chef de Projet evaluates a Collaborateur once per month
 * across 4 criteria, each scored from 0 to 5.
 */
@Entity
@Table(name = "evaluations",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"collaborateur_id", "evaluateur_id", "mois", "annee"}
       ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collaborateur_id", nullable = false)
    private User collaborateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluateur_id", nullable = false)
    private User evaluateur;

    @Column(nullable = false)
    private Integer mois;

    @Column(nullable = false)
    private Integer annee;

    @Column(nullable = false)
    private Double qualiteTravail;

    @Column(nullable = false)
    private Double respectDelais;

    @Column(nullable = false)
    private Double travailEquipe;

    @Column(nullable = false)
    private Double communication;

    @Column(length = 1000)
    private String commentaire;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }
}
