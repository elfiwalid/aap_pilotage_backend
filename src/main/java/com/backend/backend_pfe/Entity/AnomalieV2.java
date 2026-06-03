package com.backend.backend_pfe.Entity;

import com.backend.backend_pfe.enums.StatutAnomalieV2;
import com.backend.backend_pfe.enums.TypeAnomalieV2;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Anomalie de staffing détectée par le moteur V2.
 * Contient toutes les informations nécessaires pour le Resource Manager.
 */
@Entity
@Table(name = "anomalies_v2")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalieV2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAnomalieV2 typeAnomalie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAnomalieV2 statut;

    private LocalDateTime dateDetection;

    // ═══ Collaborateur concerné ═══
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collaborateur_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User collaborateur;

    private String numeroEmploye;   // matricule
    private String collaborateurNom; // nom complet pour affichage

    // ═══ Période concernée ═══
    private int annee;
    private int mois;

    // ═══ Données de charge ═══
    private int capaciteMensuelle;     // jours ouvrables validés du mois
    private int totalJoursDemandes;    // somme des jours de toutes les affectations
    private int joursDepassement;      // totalJoursDemandes - capaciteMensuelle (si > 0)
    private int joursDisponibles;      // capaciteMensuelle - totalJoursDemandes (si > 0)
    private double tauxCharge;         // totalJoursDemandes / capaciteMensuelle * 100

    // ═══ Conflits (dates de chevauchement) ═══
    private LocalDate conflitDateDebut;
    private LocalDate conflitDateFin;
    private int joursEnConflit;

    // ═══ Projets/Clients concernés ═══
    @Column(length = 2000)
    private String projetsConcernes;   // JSON ou texte séparé par "|"

    @Column(length = 2000)
    private String clientsConcernes;

    // ═══ Description lisible ═══
    @Column(length = 3000)
    private String description;

    // ═══ Clé de déduplication ═══
    // Permet d'éviter les doublons si la détection est relancée
    @Column(length = 500, unique = true)
    private String cleDeduplication;
}
