package com.backend.backend_pfe.Entity;

import com.backend.backend_pfe.enums.SimulationDecisionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_decisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationSimulation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    private SimulationWhatIf simulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_projet_id", nullable = false)
    private User chefProjet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SimulationDecisionStatus status;

    @Column(length = 2000)
    private String commentaire;

    private LocalDateTime dateDecision;
}
