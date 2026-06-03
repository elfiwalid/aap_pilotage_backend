package com.backend.backend_pfe.Entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Configuration des jours ouvrables validés par le Resource Manager.
 * Chaque entrée représente un mois/année avec le nombre de jours ouvrables.
 */
@Entity
@Table(name = "jours_ouvrables_config", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"annee", "mois", "pays"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoursOuvrablesConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int annee;
    private int mois;
    private String pays;

    /** Nombre de jours ouvrables auto-calculé (jours du mois - weekends - fériés) */
    private int joursOuvrablesAuto;

    /** Nombre de jours ouvrables validé/modifié par le RM */
    private int joursOuvrablesValide;

    /** true si le RM a validé ce mois */
    private boolean valide;
}
