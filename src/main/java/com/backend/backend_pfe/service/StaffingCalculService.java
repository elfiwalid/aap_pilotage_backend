package com.backend.backend_pfe.service;

import com.backend.backend_pfe.Entity.Affectation;
import com.backend.backend_pfe.Entity.JoursOuvrablesConfig;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.AffectationRepository;
import com.backend.backend_pfe.Repository.JoursOuvrablesConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StaffingCalculService {

    private final AffectationRepository affectationRepository;
    private final JoursOuvrablesConfigRepository joursOuvrablesConfigRepository;

    public int getCapaciteMensuelle(int annee, int mois, String pays) {
        Optional<JoursOuvrablesConfig> config =
                joursOuvrablesConfigRepository.findByAnneeAndMoisAndPays(annee, mois, pays);

        if (config.isPresent() && config.get().isValide()) {
            return config.get().getJoursOuvrablesValide();
        }

        return calculerJoursOuvrablesDuMoisSansWeekends(annee, mois);
    }

    public int countJoursOuvrables(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || fin.isBefore(debut)) {
            return 0;
        }

        int count = 0;
        LocalDate current = debut;

        while (!current.isAfter(fin)) {
            DayOfWeek day = current.getDayOfWeek();

            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                count++;
            }

            current = current.plusDays(1);
        }

        return count;
    }

    public boolean overlaps(LocalDate firstStart, LocalDate firstEnd,
                            LocalDate secondStart, LocalDate secondEnd) {
        return !firstEnd.isBefore(secondStart) && !firstStart.isAfter(secondEnd);
    }

    public double calculerJoursDemandes(User collaborateur, int annee, int mois) {
        YearMonth yearMonth = YearMonth.of(annee, mois);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        return affectationRepository.findByCollaborateur(collaborateur)
                .stream()
                .filter(affectation -> affectation.getDateDebut() != null && affectation.getDateFin() != null)
                .filter(affectation -> overlaps(
                        affectation.getDateDebut(),
                        affectation.getDateFin(),
                        monthStart,
                        monthEnd
                ))
                .mapToDouble(affectation -> calculerJoursDemandesPourAffectation(
                        affectation,
                        monthStart,
                        monthEnd
                ))
                .sum();
    }

    public double calculerTauxCharge(double totalJoursDemandes, int capaciteMensuelle) {
        if (capaciteMensuelle <= 0) {
            return 0;
        }

        return Math.round(totalJoursDemandes / capaciteMensuelle * 1000.0) / 10.0;
    }

    public String determinerEtat(double tauxCharge) {
        if (tauxCharge > 100) {
            return "SURCHARGE";
        }

        if (tauxCharge == 0) {
            return "NON_STAFFE";
        }

        if (tauxCharge < 100) {
            return "SOUS_CHARGE";
        }

        return "NORMAL";
    }

    public double calculerJoursDemandesPourPeriode(LocalDate dateDebut,
                                                   LocalDate dateFin,
                                                   Double tauxAffectation) {
        double taux = tauxAffectation != null ? tauxAffectation : 100.0;
        int joursOuvrables = countJoursOuvrables(dateDebut, dateFin);

        return joursOuvrables * taux / 100;
    }

    private double calculerJoursDemandesPourAffectation(Affectation affectation,
                                                        LocalDate monthStart,
                                                        LocalDate monthEnd) {
        LocalDate effectiveStart = affectation.getDateDebut().isBefore(monthStart)
                ? monthStart
                : affectation.getDateDebut();

        LocalDate effectiveEnd = affectation.getDateFin().isAfter(monthEnd)
                ? monthEnd
                : affectation.getDateFin();

        double taux = affectation.getTauxAffectation() != null
                ? affectation.getTauxAffectation()
                : 100.0;

        int jours = countJoursOuvrables(effectiveStart, effectiveEnd);

        return jours * taux / 100;
    }

    private int calculerJoursOuvrablesDuMoisSansWeekends(int annee, int mois) {
        YearMonth yearMonth = YearMonth.of(annee, mois);
        int total = 0;

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = LocalDate.of(annee, mois, day);
            DayOfWeek dayOfWeek = date.getDayOfWeek();

            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                total++;
            }
        }

        return total;
    }
}
