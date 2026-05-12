package com.backend.backend_pfe.Entity;


import com.backend.backend_pfe.enums.StatutProjet;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    @Column(length = 1500)
    private String description;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    private StatutProjet statut;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne
    @JoinColumn(name = "chef_projet_id")
    private User chefProjet;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL)
    private List<Affectation> affectations = new ArrayList<>();

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL)
    private List<Prevision> previsions = new ArrayList<>();

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL)
    private List<Anomalie> anomalies = new ArrayList<>();

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL)
    private List<RapportV2> rapports = new ArrayList<>();
}