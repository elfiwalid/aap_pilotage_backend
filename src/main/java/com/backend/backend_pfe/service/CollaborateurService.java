package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.CollabDashboardDTO;
import com.backend.backend_pfe.DTO.response.CollabPlanningJourDTO;
import com.backend.backend_pfe.DTO.response.CollabProjetDTO;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

/**
 * Service pour l'espace collaborateur : dashboard, projets, planning.
 */
public interface CollaborateurService {

    /** Données agrégées du dashboard (KPIs, charge mensuelle, projets). */
    CollabDashboardDTO getDashboard(Authentication authentication);

    /** Liste des projets assignés au collaborateur connecté. */
    List<CollabProjetDTO> getMesProjets(Authentication authentication);

    /** Planning par jour pour un mois/année donnés. */
    List<CollabPlanningJourDTO> getPlanning(Authentication authentication, int annee, int mois);
}
