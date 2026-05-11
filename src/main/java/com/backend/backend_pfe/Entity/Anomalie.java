package com.backend.backend_pfe.Entity;


import com.backend.backend_pfe.enums.StatutAnomalie;
import com.backend.backend_pfe.enums.TypeAnomalie;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "anomalies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anomalie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private TypeAnomalie typeAnomalie;

    @Enumerated(EnumType.STRING)
    private StatutAnomalie statut;

    private LocalDateTime dateDetection;

    @ManyToOne
    @JoinColumn(name = "projet_id")
    private Projet projet;

    @ManyToOne
    @JoinColumn(name = "collaborateur_id")
    private User collaborateur;
}