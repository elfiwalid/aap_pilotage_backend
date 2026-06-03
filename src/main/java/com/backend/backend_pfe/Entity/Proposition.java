package com.backend.backend_pfe.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Proposition de ressource alternative envoyée par le RM à un chef de projet.
 */
@Entity
@Table(name = "propositions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Le collaborateur proposé comme alternative */
    @ManyToOne
    @JoinColumn(name = "collaborateur_propose_id")
    private User collaborateurPropose;

    /** Le projet cible (celui en surcharge) */
    @ManyToOne
    @JoinColumn(name = "projet_id")
    private Projet projet;

    /** Le chef de projet destinataire */
    @ManyToOne
    @JoinColumn(name = "chef_projet_id")
    private User chefProjet;

    /** Le RM qui a fait la proposition */
    @ManyToOne
    @JoinColumn(name = "propose_par_id")
    private User proposePar;

    /** L'anomalie liée (optionnel) */
    @ManyToOne
    @JoinColumn(name = "anomalie_id")
    private Anomalie anomalie;

    private LocalDateTime dateProposition;

    /** PENDING, ACCEPTED, REJECTED */
    private String statut;

    private String message;
}
