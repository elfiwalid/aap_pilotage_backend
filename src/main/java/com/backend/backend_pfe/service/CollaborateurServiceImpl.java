package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.CollabDashboardDTO;
import com.backend.backend_pfe.DTO.response.CollabPlanningJourDTO;
import com.backend.backend_pfe.DTO.response.CollabProjetDTO;
import com.backend.backend_pfe.Entity.Affectation;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.AffectationRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implémentation du service collaborateur.
 *
 * Les données sont calculées dynamiquement à partir des affectations
 * réelles du collaborateur (pas de données mockées).
 */
@Service
@RequiredArgsConstructor
public class CollaborateurServiceImpl implements CollaborateurService {

    private final AffectationRepository affectationRepository;
    private final UserRepository userRepository;

    /** Palette de couleurs déterministe assignée par projet. */
    private static final String[] PALETTE = {
            "#7B2CBF", "#2D9CDB", "#059669", "#F59E0B",
            "#E600A9", "#8B5CF6", "#EF4444", "#0EA5E9",
            "#10B981", "#F97316", "#6366F1", "#EC4899"
    };

    @Override
    public CollabDashboardDTO getDashboard(Authentication authentication, Integer anneeParam, Integer moisParam) {
        User collaborateur = resolveUser(authentication);
        List<Affectation> affectations = affectationRepository.findByCollaborateur(collaborateur);
        LocalDate today = LocalDate.now();
        int year = anneeParam != null ? anneeParam : today.getYear();
        int month = moisParam != null ? moisParam : today.getMonthValue();

        // Use the selected month for calculations
        YearMonth selectedYm = YearMonth.of(year, month);
        LocalDate monthStart = selectedYm.atDay(1);
        LocalDate monthEnd = selectedYm.atEndOfMonth();

        // Affectations active in the selected month
        List<Affectation> actives = affectations.stream()
                .filter(a -> a.getDateDebut() != null && a.getDateFin() != null)
                .filter(a -> !a.getDateFin().isBefore(monthStart) && !a.getDateDebut().isAfter(monthEnd))
                .toList();

        List<CollabProjetDTO> projets = affectations.stream()
                .map(this::toProjetDTO)
                .sorted((a, b) -> b.getDateFin().compareTo(a.getDateFin()))
                .toList();

        // Calculate real charge based on jours ouvrables
        int monthCapacity = countWorkingDays(monthStart, monthEnd);
        int totalJours = actives.stream().mapToInt(a -> {
            LocalDate effStart = a.getDateDebut().isBefore(monthStart) ? monthStart : a.getDateDebut();
            LocalDate effEnd = a.getDateFin().isAfter(monthEnd) ? monthEnd : a.getDateFin();
            return countWorkingDays(effStart, effEnd);
        }).sum();

        double tauxCharge = monthCapacity > 0 ? Math.round((double) totalJours / monthCapacity * 1000.0) / 10.0 : 0;
        double capaciteRestante = Math.max(0, 100 - tauxCharge);

        int projetsBientotTermines = (int) actives.stream()
                .filter(a -> a.getDateFin() != null
                        && !a.getDateFin().isBefore(monthStart)
                        && a.getDateFin().isBefore(monthEnd.plusDays(30)))
                .count();

        List<CollabProjetDTO> projetsActifsDTO = projets.stream()
                .filter(p -> p.getStatut() == com.backend.backend_pfe.enums.StatutProjet.EN_COURS
                        || isActiveOnByDates(p.getDateDebut(), p.getDateFin(), monthStart))
                .toList();

        int avancementMoyen = projetsActifsDTO.isEmpty() ? 0 :
                (int) Math.round(projetsActifsDTO.stream()
                        .mapToInt(CollabProjetDTO::getAvancement)
                        .average().orElse(0));

        return CollabDashboardDTO.builder()
                .projetsAssignes(actives.size())
                .tauxCharge(tauxCharge)
                .capaciteRestante(Math.round(capaciteRestante * 10.0) / 10.0)
                .projetsBientotTermines(projetsBientotTermines)
                .avancementMoyen(avancementMoyen)
                .projets(projets)
                .chargeMensuelle(computeChargeMensuelle(affectations, selectedYm.atDay(15)))
                .build();
    }

    @Override
    public List<CollabProjetDTO> getMesProjets(Authentication authentication) {
        User collaborateur = resolveUser(authentication);
        return affectationRepository.findByCollaborateur(collaborateur).stream()
                .map(this::toProjetDTO)
                .sorted((a, b) -> b.getDateFin().compareTo(a.getDateFin()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CollabPlanningJourDTO> getPlanning(Authentication authentication, int annee, int mois) {
        User collaborateur = resolveUser(authentication);
        List<Affectation> affectations = affectationRepository.findByCollaborateur(collaborateur);

        YearMonth yearMonth = YearMonth.of(annee, mois);
        int daysInMonth = yearMonth.lengthOfMonth();
        int monthCapacity = countWorkingDays(yearMonth.atDay(1), yearMonth.atEndOfMonth());

        List<CollabPlanningJourDTO> planning = new ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(annee, mois, day);

            boolean isWeekend = date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;

            List<CollabPlanningJourDTO.SlotDTO> slots = isWeekend
                    ? new ArrayList<>()
                    : affectations.stream()
                            .filter(a -> isActiveOn(a, date))
                            .map(a -> {
                                // Calculate real % for this project in this month
                                LocalDate mStart = yearMonth.atDay(1);
                                LocalDate mEnd = yearMonth.atEndOfMonth();
                                LocalDate effStart = a.getDateDebut().isBefore(mStart) ? mStart : a.getDateDebut();
                                LocalDate effEnd = a.getDateFin().isAfter(mEnd) ? mEnd : a.getDateFin();
                                int jours = countWorkingDays(effStart, effEnd);
                                double pct = monthCapacity > 0 ? Math.round((double) jours / monthCapacity * 1000.0) / 10.0 : 0;
                                return CollabPlanningJourDTO.SlotDTO.builder()
                                        .projetId(a.getProjet().getId())
                                        .projet(a.getProjet().getNom())
                                        .couleur(colorFor(a.getProjet().getId()))
                                        .alloc(pct)
                                        .build();
                            })
                            .collect(Collectors.toList());

            planning.add(CollabPlanningJourDTO.builder()
                    .date(date)
                    .slots(slots)
                    .build());
        }
        return planning;
    }

    // ─── Helpers ───

    private CollabProjetDTO toProjetDTO(Affectation a) {
        Projet projet = a.getProjet();
        User chef = projet.getChefProjet();

        // Taille de l'équipe = collaborateurs distincts sur le projet
        int tailleEquipe = (int) affectationRepository.findByProjet(projet).stream()
                .map(aff -> aff.getCollaborateur().getId())
                .distinct()
                .count();

        // Calculate real allocation % based on jours ouvrables for the affectation month
        double realTaux = 0;
        if (a.getDateDebut() != null && a.getDateFin() != null) {
            YearMonth affYm = YearMonth.from(a.getDateDebut());
            LocalDate mStart = affYm.atDay(1);
            LocalDate mEnd = affYm.atEndOfMonth();
            int mCapacity = countWorkingDays(mStart, mEnd);
            LocalDate effStart = a.getDateDebut().isBefore(mStart) ? mStart : a.getDateDebut();
            LocalDate effEnd = a.getDateFin().isAfter(mEnd) ? mEnd : a.getDateFin();
            int jours = countWorkingDays(effStart, effEnd);
            realTaux = mCapacity > 0 ? Math.round((double) jours / mCapacity * 1000.0) / 10.0 : 0;
        }

        return CollabProjetDTO.builder()
                .id(projet.getId())
                .nom(projet.getNom())
                .description(projet.getDescription())
                .role(a.getRoleDansProjet() != null ? a.getRoleDansProjet() : "Collaborateur")
                .tauxAffectation(realTaux)
                .dateDebut(projet.getDateDebut())
                .dateFin(projet.getDateFin())
                .statut(projet.getStatut())
                .chefProjetNomComplet(chef != null ? chef.getPrenom() + " " + chef.getNom() : "N/A")
                .couleur(colorFor(projet.getId()))
                .avancement(computeAvancement(projet.getDateDebut(), projet.getDateFin()))
                .tailleEquipe(tailleEquipe)
                .build();
    }

    /**
     * Avancement basé sur le temps écoulé entre dateDebut et dateFin.
     */
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

    /**
     * Charge mensuelle sur 12 mois glissants à partir du mois courant.
     */
    private List<CollabDashboardDTO.ChargeMensuelleDTO> computeChargeMensuelle(
            List<Affectation> affectations, LocalDate today) {
        List<CollabDashboardDTO.ChargeMensuelleDTO> result = new ArrayList<>();
        YearMonth start = YearMonth.from(today);

        for (int i = 0; i < 12; i++) {
            YearMonth ym = start.plusMonths(i);
            LocalDate monthStart = ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth();
            int monthCapacity = countWorkingDays(monthStart, monthEnd);

            List<Affectation> chevauchantes = affectations.stream()
                    .filter(a -> overlaps(a, monthStart, monthEnd))
                    .toList();

            double taux = 0;
            if (monthCapacity > 0) {
                int totalJours = chevauchantes.stream().mapToInt(a -> {
                    LocalDate effStart = a.getDateDebut().isBefore(monthStart) ? monthStart : a.getDateDebut();
                    LocalDate effEnd = a.getDateFin().isAfter(monthEnd) ? monthEnd : a.getDateFin();
                    return countWorkingDays(effStart, effEnd);
                }).sum();
                taux = Math.round((double) totalJours / monthCapacity * 1000.0) / 10.0;
            }

            String moisLabel = capitalize(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH))
                    .replace(".", "");

            result.add(CollabDashboardDTO.ChargeMensuelleDTO.builder()
                    .mois(moisLabel)
                    .annee(ym.getYear())
                    .tauxCharge(taux)
                    .nombreProjets(chevauchantes.size())
                    .build());
        }
        return result;
    }

    private boolean isActiveOn(Affectation a, LocalDate date) {
        return isActiveOnByDates(a.getDateDebut(), a.getDateFin(), date);
    }

    private boolean isActiveOnByDates(LocalDate debut, LocalDate fin, LocalDate date) {
        if (debut == null || fin == null) return false;
        return !date.isBefore(debut) && !date.isAfter(fin);
    }

    private boolean overlaps(Affectation a, LocalDate start, LocalDate end) {
        if (a.getDateDebut() == null || a.getDateFin() == null) return false;
        return !a.getDateFin().isBefore(start) && !a.getDateDebut().isAfter(end);
    }

    private String colorFor(Long projetId) {
        if (projetId == null) return PALETTE[0];
        return PALETTE[(int) (projetId % PALETTE.length)];
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private User resolveUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

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
}
