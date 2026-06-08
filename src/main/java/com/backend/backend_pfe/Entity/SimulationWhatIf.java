package com.backend.backend_pfe.Entity;

import com.backend.backend_pfe.enums.ResultatSimulationWhatIf;
import com.backend.backend_pfe.enums.StatutSimulationWhatIf;
import com.backend.backend_pfe.enums.TypeSimulationWhatIf;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "simulations_what_if")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationWhatIf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_simulation", nullable = false)
    private TypeSimulationWhatIf typeSimulation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutSimulationWhatIf statut;

    @Enumerated(EnumType.STRING)
    private ResultatSimulationWhatIf resultat;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anomalie_id")
    private AnomalieV2 anomalie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_manager_id")
    private User resourceManager;

    @PrePersist
    public void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}