package com.backend.backend_pfe.Entity;


import com.backend.backend_pfe.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private String prenom;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;

    private String poste;

    @Column(unique = true)
    private String matricule;


    private Double tauxStaffing;

    private Boolean disponible;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToMany(mappedBy = "chefProjet")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Projet> projetsGeres = new ArrayList<>();

    @OneToMany(mappedBy = "collaborateur")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Affectation> affectations = new ArrayList<>();

    @OneToMany(mappedBy = "importePar")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Prevision> previsionsImportees = new ArrayList<>();

    @OneToMany(mappedBy = "collaborateur")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Anomalie> anomalies = new ArrayList<>();

    @OneToMany(mappedBy = "destinataire")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Notification> notificationsRecues = new ArrayList<>();

    @OneToMany(mappedBy = "expediteur")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Notification> notificationsEnvoyees = new ArrayList<>();

    @OneToMany(mappedBy = "collaborateur")
    private List<Evaluation> evaluationsRecues = new ArrayList<>();

    @OneToMany(mappedBy = "evaluateur")
    private List<Evaluation> evaluationsDonnees = new ArrayList<>();
}