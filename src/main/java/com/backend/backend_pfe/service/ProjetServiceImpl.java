package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.ProjetRequestDTO;
import com.backend.backend_pfe.DTO.response.DashboardChefProjetDTO;
import com.backend.backend_pfe.DTO.response.ProjetResponseDTO;
import com.backend.backend_pfe.Entity.*;
import com.backend.backend_pfe.Repository.*;
import com.backend.backend_pfe.enums.StatutAnomalieV2;
import com.backend.backend_pfe.enums.StatutProjet;
import com.backend.backend_pfe.enums.TypeAnomalieV2;
import com.backend.backend_pfe.exception.BusinessValidationException;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the ProjetService interface.
 *
 * SOLID — Single Responsibility Principle (SRP):
 *   This class contains exclusively the business logic for project creation:
 *   validation rules, repository calls, and DTO mapping.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 *   Depends on repository abstractions (interfaces) injected via constructor.
 */
@Service
@RequiredArgsConstructor
public class ProjetServiceImpl implements ProjetService {

    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AffectationRepository affectationRepository;
    private final AnomalieV2Repository anomalieV2Repository;

    @Override
    public ProjetResponseDTO creerProjet(ProjetRequestDTO request, Authentication authentication) {
        // 1. Extraire l'email depuis le contexte d'authentification
        String email = authentication.getName();

        // 2. Rechercher l'utilisateur en base
        User chefProjet = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chef de projet introuvable"));

        // 3. Validation métier : cohérence des dates
        if (request.getDateFin().isBefore(request.getDateDebut())
                || request.getDateFin().isEqual(request.getDateDebut())) {
            throw new BusinessValidationException(
                    "La date de fin doit être postérieure à la date de début");
        }

        // 4. Appliquer le statut par défaut si non fourni
        StatutProjet statut = request.getStatut() != null
                ? request.getStatut()
                : StatutProjet.PLANIFIE;

        // 5. Mapper DTO → Entity
        Projet projet = Projet.builder()
                .nom(request.getNom().trim())
                .description(request.getDescription())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .statut(statut)
                .chefProjet(chefProjet)
                .build();

        // 6. Persister
        Projet saved = projetRepository.save(projet);

        // 6b. Notifier le chef de projet de la création
        notificationService.creerNotification(
                chefProjet, null,
                com.backend.backend_pfe.enums.TypeNotification.PROJET,
                "Projet créé — " + saved.getNom(),
                String.format("Le projet « %s » a été créé avec succès (statut : %s).",
                        saved.getNom(), statut.name()),
                null);

        // 7. Mapper Entity → ResponseDTO
        return mapToResponseDTO(saved);
    }

    @Override
    public List<ProjetResponseDTO> getMesProjets(Authentication authentication) {
        String email = authentication.getName();

        User chefProjet = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chef de projet introuvable"));

        List<Projet> projets = projetRepository.findByChefProjet(chefProjet);

        return projets.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DashboardChefProjetDTO getDashboard(Authentication authentication, Integer pAnnee, Integer pMois) {
        String email = authentication.getName();
        User chef = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Chef de projet introuvable"));

        List<Projet> mesProjets = projetRepository.findByChefProjet(chef);
        LocalDate today = LocalDate.now();

        // ─── Comptages de projets ────────────────────────────────────
        long actifs    = mesProjets.stream().filter(p -> p.getStatut() == StatutProjet.EN_COURS).count();
        long termines  = mesProjets.stream().filter(p -> p.getStatut() == StatutProjet.TERMINE).count();
        long enAttente = mesProjets.stream().filter(p -> p.getStatut() == StatutProjet.PLANIFIE
                || p.getStatut() == StatutProjet.SUSPENDU).count();

        // ─── Collaborateurs distincts sur tous les projets actifs ────
        Set<Long> collabIds = mesProjets.stream()
                .filter(p -> p.getStatut() == StatutProjet.EN_COURS)
                .flatMap(p -> affectationRepository.findByProjet(p).stream())
                .map(a -> a.getCollaborateur().getId())
                .collect(Collectors.toSet());
        int totalCollabs = collabIds.size();

        // ─── Période cible (paramètres ou mois courant) ─────────────
        int annee = pAnnee != null ? pAnnee : today.getYear();
        int mois  = pMois  != null ? pMois  : today.getMonthValue();

        // Tous les IDs collaborateurs des projets du chef (pour filtrer)
        YearMonth ym = YearMonth.of(annee, mois);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd   = ym.atEndOfMonth();

        Set<Long> mesCollabIds = mesProjets.stream()
                .flatMap(p -> affectationRepository.findByProjet(p).stream())
                .filter(a -> a.getDateDebut() != null && a.getDateFin() != null)
                .filter(a -> !a.getDateFin().isBefore(monthStart) && !a.getDateDebut().isAfter(monthEnd))
                .map(a -> a.getCollaborateur().getId())
                .collect(Collectors.toSet());

        List<AnomalieV2> anomaliesMois = anomalieV2Repository
                .findByAnneeAndMoisOrderByDateDetectionDesc(annee, mois).stream()
                .filter(a -> a.getCollaborateur() != null && mesCollabIds.contains(a.getCollaborateur().getId()))
                .toList();

        long anomaliesCritiques = anomaliesMois.stream()
                .filter(a -> a.getTypeAnomalie() == TypeAnomalieV2.SURCHARGE
                          || a.getTypeAnomalie() == TypeAnomalieV2.CONFLIT)
                .count();
        long anomaliesActives = anomaliesMois.stream()
                .filter(a -> a.getStatut() == StatutAnomalieV2.DETECTEE
                          || a.getStatut() == StatutAnomalieV2.EN_COURS_TRAITEMENT)
                .count();

        // ─── Tendance anomalies sur 6 mois ──────────────────────────
        List<DashboardChefProjetDTO.MoisAnomalieDTO> tendance = new ArrayList<>();
        String[] MOIS_LABELS = {"Jan","Fév","Mar","Avr","Mai","Jun","Jul","Aoû","Sep","Oct","Nov","Déc"};
        for (int i = 5; i >= 0; i--) {
            YearMonth ymIt = YearMonth.of(annee, mois).minusMonths(i);
            int a = ymIt.getYear(), m = ymIt.getMonthValue();

            // Collab ids pour ce mois
            LocalDate ms = ymIt.atDay(1);
            LocalDate me = ymIt.atEndOfMonth();
            Set<Long> cIds = mesProjets.stream()
                    .flatMap(p -> affectationRepository.findByProjet(p).stream())
                    .filter(af -> af.getDateDebut() != null && af.getDateFin() != null)
                    .filter(af -> !af.getDateFin().isBefore(ms) && !af.getDateDebut().isAfter(me))
                    .map(af -> af.getCollaborateur().getId())
                    .collect(Collectors.toSet());

            List<AnomalieV2> anomMois = anomalieV2Repository
                    .findByAnneeAndMoisOrderByDateDetectionDesc(a, m).stream()
                    .filter(an -> an.getCollaborateur() != null && cIds.contains(an.getCollaborateur().getId()))
                    .toList();

            tendance.add(DashboardChefProjetDTO.MoisAnomalieDTO.builder()
                    .mois(MOIS_LABELS[m - 1])
                    .annee(a)
                    .moisNum(m)
                    .total(anomMois.size())
                    .surcharges((int) anomMois.stream().filter(an -> an.getTypeAnomalie() == TypeAnomalieV2.SURCHARGE).count())
                    .conflits((int) anomMois.stream().filter(an -> an.getTypeAnomalie() == TypeAnomalieV2.CONFLIT).count())
                    .sousCharges((int) anomMois.stream().filter(an -> an.getTypeAnomalie() == TypeAnomalieV2.SOUS_CHARGE).count())
                    .build());
        }

        // ─── Évolution des collaborateurs (6 mois) ──────────────────
        List<DashboardChefProjetDTO.MoisCollabDTO> evolutionCollabs = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ymIt = YearMonth.of(annee, mois).minusMonths(i);
            int a = ymIt.getYear(), m = ymIt.getMonthValue();
            LocalDate ms = ymIt.atDay(1);
            LocalDate me = ymIt.atEndOfMonth();

            long nbCollabs = mesProjets.stream()
                    .flatMap(p -> affectationRepository.findByProjet(p).stream())
                    .filter(af -> af.getDateDebut() != null && af.getDateFin() != null)
                    .filter(af -> !af.getDateFin().isBefore(ms) && !af.getDateDebut().isAfter(me))
                    .map(af -> af.getCollaborateur().getId())
                    .distinct().count();

            evolutionCollabs.add(DashboardChefProjetDTO.MoisCollabDTO.builder()
                    .mois(MOIS_LABELS[m - 1])
                    .annee(a)
                    .moisNum(m)
                    .collaborateurs((int) nbCollabs)
                    .build());
        }

        // ─── Performance par projet ──────────────────────────────────
        List<DashboardChefProjetDTO.ProjetPerfDTO> performance = mesProjets.stream()
                .map(p -> {
                    int avancement = calcAvancement(p, today);
                    int nbCollabProjet = (int) affectationRepository.findByProjet(p).stream()
                            .map(a -> a.getCollaborateur().getId()).distinct().count();
                    return DashboardChefProjetDTO.ProjetPerfDTO.builder()
                            .id(p.getId())
                            .nom(p.getNom())
                            .avancementPct(avancement)
                            .collaborateurs(nbCollabProjet)
                            .statut(p.getStatut())
                            .dateDebut(p.getDateDebut())
                            .dateFin(p.getDateFin())
                            .build();
                })
                .toList();

        // ─── 5 anomalies récentes ────────────────────────────────────
        List<DashboardChefProjetDTO.AnomalieResumeeDTO> anomaliesRecentes = anomaliesMois.stream()
                .limit(5)
                .map(a -> DashboardChefProjetDTO.AnomalieResumeeDTO.builder()
                        .id(a.getId())
                        .collaborateurNom(a.getCollaborateurNom())
                        .typeAnomalie(a.getTypeAnomalie().name())
                        .statut(a.getStatut().name())
                        .tauxCharge(a.getTauxCharge())
                        .projetsConcernes(a.getProjetsConcernes())
                        .annee(a.getAnnee())
                        .mois(a.getMois())
                        .dateDetection(a.getDateDetection() != null
                                ? a.getDateDetection().toString() : null)
                        .build())
                .toList();

        // ─── Projets résumés ────────────────────────────────────────
        List<DashboardChefProjetDTO.ProjetResumeeDTO> projetsResumes = mesProjets.stream()
                .map(p -> {
                    int nbColl = (int) affectationRepository.findByProjet(p).stream()
                            .map(a -> a.getCollaborateur().getId()).distinct().count();
                    return DashboardChefProjetDTO.ProjetResumeeDTO.builder()
                            .id(p.getId())
                            .nom(p.getNom())
                            .statut(p.getStatut())
                            .dateDebut(p.getDateDebut())
                            .dateFin(p.getDateFin())
                            .avancementPct(calcAvancement(p, today))
                            .collaborateurs(nbColl)
                            .build();
                })
                .toList();

        return DashboardChefProjetDTO.builder()
                .totalProjets(mesProjets.size())
                .projetsActifs((int) actifs)
                .projetsTermines((int) termines)
                .projetsEnAttente((int) enAttente)
                .totalCollaborateurs(totalCollabs)
                .totalAnomaliesMoisCourant(anomaliesMois.size())
                .anomaliesCritiques((int) anomaliesCritiques)
                .anomaliesActives((int) anomaliesActives)
                .evolutionCollaborateurs(evolutionCollabs)
                .performanceProjets(performance)
                .tendanceAnomalies(tendance)
                .anomaliesRecentes(anomaliesRecentes)
                .projetsRecents(projetsResumes)
                .build();
    }

    /** Calcule le % d'avancement basé sur la durée écoulée (0-100). */
    private int calcAvancement(Projet p, LocalDate today) {
        if (p.getStatut() == StatutProjet.TERMINE) return 100;
        if (p.getStatut() == StatutProjet.PLANIFIE) return 0;
        if (p.getDateDebut() == null || p.getDateFin() == null) return 0;
        if (today.isBefore(p.getDateDebut())) return 0;
        if (!today.isBefore(p.getDateFin())) return 100;
        long total = p.getDateDebut().until(p.getDateFin(), java.time.temporal.ChronoUnit.DAYS);
        if (total == 0) return 100;
        long elapsed = p.getDateDebut().until(today, java.time.temporal.ChronoUnit.DAYS);
        return (int) Math.min(100, Math.round(elapsed * 100.0 / total));
    }

    private ProjetResponseDTO mapToResponseDTO(Projet projet) {
        User chef = projet.getChefProjet();
        String nomComplet = chef.getNom() + " " + chef.getPrenom();

        return ProjetResponseDTO.builder()
                .id(projet.getId())
                .nom(projet.getNom())
                .description(projet.getDescription())
                .dateDebut(projet.getDateDebut())
                .dateFin(projet.getDateFin())
                .statut(projet.getStatut())
                .chefProjetId(chef.getId())
                .chefProjetNomComplet(nomComplet)
                .dateCreation(projet.getDateCreation())
                .build();
    }
}
