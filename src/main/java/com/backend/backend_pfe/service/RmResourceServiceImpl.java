package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.RmConflitDTO;
import com.backend.backend_pfe.DTO.response.RmDashboardDTO;
import com.backend.backend_pfe.DTO.response.RmProjetDTO;
import com.backend.backend_pfe.DTO.response.RmResourceDTO;
import com.backend.backend_pfe.Entity.*;
import com.backend.backend_pfe.Repository.*;
import com.backend.backend_pfe.enums.Role;
import com.backend.backend_pfe.enums.StatutAnomalie;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation du service RM Resources.
 */
@Service
@RequiredArgsConstructor
public class RmResourceServiceImpl implements RmResourceService {

    private final UserRepository userRepository;
    private final AffectationRepository affectationRepository;
    private final ProjetRepository projetRepository;
    private final AnomalieRepository anomalieRepository;
    private final PropositionRepository propositionRepository;
    private final NotificationService notificationService;

    private static final String[] PALETTE = {
            "#7B2CBF", "#2D9CDB", "#059669", "#F59E0B",
            "#E600A9", "#8B5CF6", "#EF4444", "#0EA5E9",
            "#10B981", "#F97316", "#6366F1", "#EC4899"
    };

    @Override
    @Transactional(readOnly = true)
    public List<RmResourceDTO> getAllResources(Integer annee, Integer mois) {
        List<User> collaborateurs = userRepository.findByRole(Role.COLLABORATEUR);
        LocalDate today = LocalDate.now();
        int year = annee != null ? annee : today.getYear();
        int month = mois != null ? mois : today.getMonthValue();

        return collaborateurs.stream()
                .map(user -> buildResourceDTO(user, year, month))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RmProjetDTO> getAllProjets() {
        List<Projet> projets = projetRepository.findAll();
        return projets.stream()
                .map(this::buildProjetDTO)
                .sorted((a, b) -> {
                    // En cours d'abord, puis planifié, puis terminé
                    int order = statusOrder(a.getStatut()) - statusOrder(b.getStatut());
                    if (order != 0) return order;
                    return b.getDateDebut().compareTo(a.getDateDebut());
                })
                .collect(Collectors.toList());
    }

    private int statusOrder(com.backend.backend_pfe.enums.StatutProjet s) {
        return switch (s) {
            case EN_COURS -> 0;
            case PLANIFIE -> 1;
            case SUSPENDU -> 2;
            case TERMINE -> 3;
        };
    }

    private RmProjetDTO buildProjetDTO(Projet projet) {
        User chef = projet.getChefProjet();
        List<Affectation> affectations = affectationRepository.findByProjet(projet);

        List<RmProjetDTO.MembreEquipeDTO> equipe = affectations.stream()
                .map(a -> RmProjetDTO.MembreEquipeDTO.builder()
                        .id(a.getCollaborateur().getId())
                        .nom(a.getCollaborateur().getNom())
                        .prenom(a.getCollaborateur().getPrenom())
                        .role(a.getRoleDansProjet() != null ? a.getRoleDansProjet() : "Collaborateur")
                        .tauxAffectation(a.getTauxAffectation() != null ? a.getTauxAffectation() : 0.0)
                        .build())
                .collect(Collectors.toList());

        return RmProjetDTO.builder()
                .id(projet.getId())
                .nom(projet.getNom())
                .description(projet.getDescription())
                .dateDebut(projet.getDateDebut())
                .dateFin(projet.getDateFin())
                .statut(projet.getStatut())
                .chefProjetNomComplet(chef != null ? chef.getPrenom() + " " + chef.getNom() : "N/A")
                .avancement(computeAvancement(projet.getDateDebut(), projet.getDateFin()))
                .equipe(equipe)
                .build();
    }

    // ═══════════════════════════════════════════════════════
    // DASHBOARD
    // ═══════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public RmDashboardDTO getDashboard() {
        LocalDate today = LocalDate.now();
        List<User> collabs = userRepository.findByRole(Role.COLLABORATEUR);
        List<Projet> projets = projetRepository.findAll();
        List<Anomalie> anomaliesOuvertes = anomalieRepository.findByStatut(StatutAnomalie.OUVERTE);

        // Collaborateurs actifs (au moins 1 affectation active aujourd'hui)
        int actifs = 0;
        int surcharges = 0;
        int sousUtilises = 0;
        double sumTaux = 0;

        for (User u : collabs) {
            double taux = affectationRepository.findByCollaborateur(u).stream()
                    .filter(a -> isActiveOn(a, today))
                    .mapToDouble(a -> a.getTauxAffectation() != null ? a.getTauxAffectation() : 0.0)
                    .sum();
            if (taux > 0) {
                actifs++;
                sumTaux += taux;
                if (taux > 100) surcharges++;
                else if (taux < 80) sousUtilises++;
            }
        }

        double tauxStaffing = actifs > 0 ? Math.round(sumTaux / actifs * 10.0) / 10.0 : 0;

        // Répartition projets
        int enCours = (int) projets.stream().filter(p -> p.getStatut() == com.backend.backend_pfe.enums.StatutProjet.EN_COURS).count();
        int planifies = (int) projets.stream().filter(p -> p.getStatut() == com.backend.backend_pfe.enums.StatutProjet.PLANIFIE).count();
        int termines = (int) projets.stream().filter(p -> p.getStatut() == com.backend.backend_pfe.enums.StatutProjet.TERMINE).count();

        // Top 5 anomalies
        List<RmDashboardDTO.AnomalieResumeDTO> top5 = anomaliesOuvertes.stream()
                .limit(5)
                .map(a -> {
                    User collab = a.getCollaborateur();
                    double charge = affectationRepository.findByCollaborateur(collab).stream()
                            .filter(aff -> isActiveOn(aff, today))
                            .mapToDouble(aff -> aff.getTauxAffectation() != null ? aff.getTauxAffectation() : 0.0)
                            .sum();
                    String projetsStr = affectationRepository.findByCollaborateur(collab).stream()
                            .filter(aff -> isActiveOn(aff, today))
                            .map(aff -> aff.getProjet().getNom())
                            .distinct()
                            .collect(Collectors.joining(", "));
                    String sev = charge > 150 ? "critical" : charge > 100 ? "high" : "medium";
                    return RmDashboardDTO.AnomalieResumeDTO.builder()
                            .id(a.getId())
                            .type(a.getTypeAnomalie().name())
                            .collaborateur(collab.getPrenom() + " " + collab.getNom())
                            .projets(projetsStr)
                            .charge(charge)
                            .severite(sev)
                            .build();
                })
                .collect(Collectors.toList());

        return RmDashboardDTO.builder()
                .totalCollaborateurs(collabs.size())
                .collaborateursActifs(actifs)
                .tauxStaffingGlobal(tauxStaffing)
                .conflitsDetectes(anomaliesOuvertes.size())
                .ressourcesSurchargees(surcharges)
                .ressourcesSousUtilisees(sousUtilises)
                .projetsEnCours(enCours)
                .projetsPlanifies(planifies)
                .projetsTermines(termines)
                .anomaliesActives(top5)
                .staffingMensuel(computeStaffingMensuel(collabs, today))
                .anomaliesMensuelles(computeAnomaliesMensuelles(collabs, today))
                .build();
    }

    /** Calcule le taux de staffing moyen pour chacun des 6 derniers mois */
    private List<RmDashboardDTO.MoisStaffingDTO> computeStaffingMensuel(List<User> collabs, LocalDate today) {
        List<RmDashboardDTO.MoisStaffingDTO> result = new ArrayList<>();
        String[] MOIS_LABELS = {"Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"};

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.from(today).minusMonths(i);
            LocalDate midMonth = ym.atDay(15); // milieu du mois comme référence

            int actifsMois = 0;
            double sumTaux = 0;
            for (User u : collabs) {
                double taux = affectationRepository.findByCollaborateur(u).stream()
                        .filter(a -> overlapsMonth(a, ym))
                        .mapToDouble(a -> a.getTauxAffectation() != null ? a.getTauxAffectation() : 0.0)
                        .sum();
                if (taux > 0) {
                    actifsMois++;
                    sumTaux += Math.min(taux, 100); // cap à 100 pour le taux de staffing
                }
            }
            double tauxMois = actifsMois > 0 ? Math.round(sumTaux / actifsMois * 10.0) / 10.0 : 0;

            result.add(RmDashboardDTO.MoisStaffingDTO.builder()
                    .mois(MOIS_LABELS[ym.getMonthValue() - 1])
                    .tauxStaffing(tauxMois)
                    .objectif(90.0)
                    .build());
        }
        return result;
    }

    /** Calcule le nombre d'anomalies par type pour chacun des 6 derniers mois */
    private List<RmDashboardDTO.MoisAnomaliesDTO> computeAnomaliesMensuelles(List<User> collabs, LocalDate today) {
        List<RmDashboardDTO.MoisAnomaliesDTO> result = new ArrayList<>();
        String[] MOIS_LABELS = {"Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"};

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.from(today).minusMonths(i);
            int surchargeCount = 0;
            int sousCount = 0;
            int conflitCount = 0;

            for (User u : collabs) {
                double taux = affectationRepository.findByCollaborateur(u).stream()
                        .filter(a -> overlapsMonth(a, ym))
                        .mapToDouble(a -> a.getTauxAffectation() != null ? a.getTauxAffectation() : 0.0)
                        .sum();
                long nbProjets = affectationRepository.findByCollaborateur(u).stream()
                        .filter(a -> overlapsMonth(a, ym))
                        .map(a -> a.getProjet().getId())
                        .distinct()
                        .count();

                if (taux > 100) surchargeCount++;
                else if (taux > 0 && taux < 50) sousCount++;
                if (nbProjets >= 2) conflitCount++;
            }

            result.add(RmDashboardDTO.MoisAnomaliesDTO.builder()
                    .mois(MOIS_LABELS[ym.getMonthValue() - 1])
                    .surcharge(surchargeCount)
                    .sousUtilisation(sousCount)
                    .conflit(conflitCount)
                    .build());
        }
        return result;
    }

    private boolean overlapsMonth(Affectation a, YearMonth ym) {
        if (a.getDateDebut() == null || a.getDateFin() == null) return false;
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        return !a.getDateFin().isBefore(monthStart) && !a.getDateDebut().isAfter(monthEnd);
    }

    private int computeAvancement(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null) return 0;
        LocalDate today = LocalDate.now();
        if (today.isBefore(debut)) return 0;
        if (!today.isBefore(fin)) return 100;
        long total = ChronoUnit.DAYS.between(debut, fin);
        if (total <= 0) return 100;
        long ecoule = ChronoUnit.DAYS.between(debut, today);
        return (int) Math.min(100, Math.max(0, (ecoule * 100) / total));
    }

    private RmResourceDTO buildResourceDTO(User user, int year, int month) {
        List<Affectation> affectations = affectationRepository.findByCollaborateur(user);

        // Taux utilisation and projets based on the selected month
        YearMonth selectedYm = YearMonth.of(year, month);
        LocalDate monthStart = selectedYm.atDay(1);
        LocalDate monthEnd = selectedYm.atEndOfMonth();
        int monthCapacity = countWorkingDays(monthStart, monthEnd);

        // Affectations active in the selected month
        List<Affectation> actives = affectations.stream()
                .filter(a -> overlaps(a, monthStart, monthEnd))
                .toList();

        double tauxUtilisation = 0;
        if (monthCapacity > 0) {
            int totalJoursSelectedMonth = actives.stream()
                    .mapToInt(a -> {
                        LocalDate effectiveStart = a.getDateDebut().isBefore(monthStart) ? monthStart : a.getDateDebut();
                        LocalDate effectiveEnd = a.getDateFin().isAfter(monthEnd) ? monthEnd : a.getDateFin();
                        return countWorkingDays(effectiveStart, effectiveEnd);
                    })
                    .sum();
            tauxUtilisation = Math.round((double) totalJoursSelectedMonth / monthCapacity * 1000.0) / 10.0;
        }

        List<RmResourceDTO.ProjetAffecteDTO> projets = actives.stream()
                .map(a -> {
                    LocalDate effectiveStart = a.getDateDebut().isBefore(monthStart) ? monthStart : a.getDateDebut();
                    LocalDate effectiveEnd = a.getDateFin().isAfter(monthEnd) ? monthEnd : a.getDateFin();
                    int joursProjet = countWorkingDays(effectiveStart, effectiveEnd);
                    double pctProjet = monthCapacity > 0 ? Math.round((double) joursProjet / monthCapacity * 1000.0) / 10.0 : 0;
                    return RmResourceDTO.ProjetAffecteDTO.builder()
                            .projetId(a.getProjet().getId())
                            .projetNom(a.getProjet().getNom())
                            .tauxAffectation(pctProjet)
                            .couleur(colorFor(a.getProjet().getId()))
                            .build();
                })
                .collect(Collectors.toList());

        // Heatmap : 12 mois (Jan-Déc) — based on jours ouvrables
        List<Double> heatmap = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            LocalDate mStart = ym.atDay(1);
            LocalDate mEnd = ym.atEndOfMonth();
            int mCapacity = countWorkingDays(mStart, mEnd);

            if (mCapacity == 0) {
                heatmap.add(0.0);
                continue;
            }

            int totalJours = affectations.stream()
                    .filter(a -> overlaps(a, mStart, mEnd))
                    .mapToInt(a -> {
                        LocalDate effStart = a.getDateDebut().isBefore(mStart) ? mStart : a.getDateDebut();
                        LocalDate effEnd = a.getDateFin().isAfter(mEnd) ? mEnd : a.getDateFin();
                        return countWorkingDays(effStart, effEnd);
                    })
                    .sum();

            double monthPct = Math.round((double) totalJours / mCapacity * 1000.0) / 10.0;
            heatmap.add(monthPct);
        }

        return RmResourceDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .poste(user.getPoste())
                .matricule(user.getMatricule())
                .tauxUtilisation(tauxUtilisation)
                .disponible(user.getDisponible() != null ? user.getDisponible() : true)
                .projets(projets)
                .heatmap(heatmap)
                .build();
    }

    /** Count working days (excluding weekends) between two dates inclusive. */
    private int countWorkingDays(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) return 0;
        int count = 0;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            java.time.DayOfWeek dow = d.getDayOfWeek();
            if (dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY) {
                count++;
            }
            d = d.plusDays(1);
        }
        return count;
    }

    private boolean isActiveOn(Affectation a, LocalDate date) {
        if (a.getDateDebut() == null || a.getDateFin() == null) return false;
        return !date.isBefore(a.getDateDebut()) && !date.isAfter(a.getDateFin());
    }

    private boolean overlaps(Affectation a, LocalDate start, LocalDate end) {
        if (a.getDateDebut() == null || a.getDateFin() == null) return false;
        return !a.getDateFin().isBefore(start) && !a.getDateDebut().isAfter(end);
    }

    private String colorFor(Long projetId) {
        if (projetId == null) return PALETTE[0];
        return PALETTE[(int) (projetId % PALETTE.length)];
    }

    // ═══════════════════════════════════════════════════════
    // CONFLITS
    // ═══════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<RmConflitDTO> getConflits() {
        LocalDate today = LocalDate.now();
        List<Anomalie> anomalies = anomalieRepository.findByStatut(StatutAnomalie.OUVERTE);

        // Collaborateurs sous-utilisés (< 80%) pour les alternatives
        List<User> sousUtilises = getSousUtilises(today);

        return anomalies.stream()
                .map(a -> buildConflitDTO(a, today, sousUtilises))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void proposerAlternative(Long anomalieId, Long collaborateurId, Long projetId,
                                    Authentication authentication) {
        LocalDate today = LocalDate.now();
        String email = authentication.getName();
        User rm = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        User collab = userRepository.findById(collaborateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborateur introuvable"));

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));

        Anomalie anomalie = anomalieId != null
                ? anomalieRepository.findById(anomalieId).orElse(null)
                : null;

        User chef = projet.getChefProjet();

        // Créer la proposition
        Proposition prop = Proposition.builder()
                .collaborateurPropose(collab)
                .projet(projet)
                .chefProjet(chef)
                .proposePar(rm)
                .anomalie(anomalie)
                .dateProposition(LocalDateTime.now())
                .statut("PENDING")
                .message(String.format("Le RM propose %s %s pour le projet « %s » (disponibilité: %.0f%%)",
                        collab.getPrenom(), collab.getNom(), projet.getNom(),
                        getDisponibilite(collab, today)))
                .build();
        propositionRepository.save(prop);

        // Notifier le chef de projet
        if (chef != null) {
            notificationService.creerNotification(
                    chef, rm,
                    com.backend.backend_pfe.enums.TypeNotification.PROJET,
                    "Proposition de ressource — " + projet.getNom(),
                    String.format("%s %s vous propose %s %s pour le projet « %s ».",
                            rm.getPrenom(), rm.getNom(),
                            collab.getPrenom(), collab.getNom(),
                            projet.getNom()),
                    anomalie);
        }
    }

    private RmConflitDTO buildConflitDTO(Anomalie a, LocalDate today, List<User> sousUtilises) {
        User collab = a.getCollaborateur();
        Projet projet = a.getProjet();
        User chef = projet.getChefProjet();

        // Récupérer toutes les affectations actives du collaborateur
        List<Affectation> affectations = affectationRepository.findByCollaborateur(collab).stream()
                .filter(aff -> isActiveOn(aff, today))
                .toList();

        double tauxCharge = affectations.stream()
                .mapToDouble(aff -> aff.getTauxAffectation() != null ? aff.getTauxAffectation() : 0.0)
                .sum();

        boolean isSurcharge = tauxCharge > 100;
        String type = isSurcharge ? "surcharge" : "sous-utilisation";
        String severite = tauxCharge > 150 ? "critical" : tauxCharge > 100 ? "high" : tauxCharge < 50 ? "low" : "medium";

        List<RmConflitDTO.ProjetImplique> projetsImpliques = affectations.stream()
                .map(aff -> RmConflitDTO.ProjetImplique.builder()
                        .projetId(aff.getProjet().getId())
                        .nom(aff.getProjet().getNom())
                        .chefProjet(aff.getProjet().getChefProjet() != null
                                ? aff.getProjet().getChefProjet().getPrenom() + " " + aff.getProjet().getChefProjet().getNom()
                                : "N/A")
                        .charge(aff.getTauxAffectation() != null ? aff.getTauxAffectation() : 0.0)
                        .couleur(colorFor(aff.getProjet().getId()))
                        .build())
                .collect(Collectors.toList());

        // Trouver le projet avec le taux le plus élevé (pour proposer des alternatives)
        Long projetCibleId = affectations.stream()
                .max((a1, a2) -> Double.compare(
                        a1.getTauxAffectation() != null ? a1.getTauxAffectation() : 0,
                        a2.getTauxAffectation() != null ? a2.getTauxAffectation() : 0))
                .map(aff -> aff.getProjet().getId())
                .orElse(projet.getId());

        // Alternatives : collabs sous-utilisés (< 80%)
        List<RmConflitDTO.AlternativeDTO> alternatives = isSurcharge
                ? sousUtilises.stream()
                        .filter(u -> !u.getId().equals(collab.getId()))
                        .limit(4)
                        .map(u -> RmConflitDTO.AlternativeDTO.builder()
                                .collaborateurId(u.getId())
                                .nom(u.getNom())
                                .prenom(u.getPrenom())
                                .poste(u.getPoste())
                                .disponibilite(getDisponibilite(u, today))
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        String periode = projet.getDateDebut() != null && projet.getDateFin() != null
                ? projet.getDateDebut() + " → " + projet.getDateFin()
                : "N/A";

        return RmConflitDTO.builder()
                .id(a.getId())
                .collaborateur(collab.getPrenom() + " " + collab.getNom())
                .collaborateurEmail(collab.getEmail())
                .role(collab.getPoste() != null ? collab.getPoste() : "Collaborateur")
                .chefProjet(chef != null ? chef.getPrenom() + " " + chef.getNom() : "N/A")
                .tauxCharge(tauxCharge)
                .type(type)
                .severite(severite)
                .message(a.getDescription())
                .periode(periode)
                .projets(projetsImpliques)
                .alternatives(alternatives)
                .build();
    }

    private List<User> getSousUtilises(LocalDate today) {
        return userRepository.findByRole(Role.COLLABORATEUR).stream()
                .filter(u -> {
                    double taux = affectationRepository.findByCollaborateur(u).stream()
                            .filter(a -> isActiveOn(a, today))
                            .mapToDouble(a -> a.getTauxAffectation() != null ? a.getTauxAffectation() : 0.0)
                            .sum();
                    return taux < 80;
                })
                .sorted((a, b) -> {
                    double tauxA = getTauxUtilisation(a, today);
                    double tauxB = getTauxUtilisation(b, today);
                    return Double.compare(tauxA, tauxB); // les moins chargés en premier
                })
                .collect(Collectors.toList());
    }

    private double getDisponibilite(User u, LocalDate today) {
        double taux = getTauxUtilisation(u, today);
        return Math.max(0, 100 - taux);
    }

    private double getTauxUtilisation(User u, LocalDate today) {
        return affectationRepository.findByCollaborateur(u).stream()
                .filter(a -> isActiveOn(a, today))
                .mapToDouble(a -> a.getTauxAffectation() != null ? a.getTauxAffectation() : 0.0)
                .sum();
    }
}
