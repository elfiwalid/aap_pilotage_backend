package com.backend.backend_pfe.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "scenarios_what_if")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioWhatIf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id")
    private SimulationWhatIf simulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collaborateur_source_id")
    private User collaborateurSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collaborateur_cible_id")
    private User collaborateurCible;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id")
    private Projet projet;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "taux_affectation")
    private Double tauxAffectation;

    @Column(name = "jours_source_avant")
    private Double joursSourceAvant;

    @Column(name = "jours_source_apres")
    private Double joursSourceApres;

    @Column(name = "jours_cible_avant")
    private Double joursCibleAvant;

    @Column(name = "jours_cible_apres")
    private Double joursCibleApres;

    @Column(name = "taux_source_avant")
    private Double tauxSourceAvant;

    @Column(name = "taux_source_apres")
    private Double tauxSourceApres;

    @Column(name = "taux_cible_avant")
    private Double tauxCibleAvant;

    @Column(name = "taux_cible_apres")
    private Double tauxCibleApres;

    @Column(name = "conflit_corrige")
    private Boolean conflitCorrige;

    @Column(name = "nouvelle_surcharge")
    private Boolean nouvelleSurcharge;

    @Column(name = "nouveau_conflit")
    private Boolean nouveauConflit;

    @Column(name = "sous_charge_reduite")
    private Boolean sousChargeReduite;

    @Column(columnDefinition = "TEXT")
    private String commentaire;
}