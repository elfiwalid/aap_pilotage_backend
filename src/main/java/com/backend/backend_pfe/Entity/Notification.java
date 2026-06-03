package com.backend.backend_pfe.Entity;

import com.backend.backend_pfe.enums.StatutNotification;
import com.backend.backend_pfe.enums.TypeNotification;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    @Column(length = 2000)
    private String message;

    private LocalDateTime dateCreation;

    @Enumerated(EnumType.STRING)
    private StatutNotification statut;

    @Enumerated(EnumType.STRING)
    private TypeNotification type;

    @ManyToOne
    @JoinColumn(name = "expediteur_id")
    private User expediteur;

    @ManyToOne
    @JoinColumn(name = "destinataire_id")
    private User destinataire;

    @ManyToOne
    @JoinColumn(name = "anomalie_id")
    private Anomalie anomalie;
}