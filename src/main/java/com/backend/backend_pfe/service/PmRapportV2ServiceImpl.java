package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.PmRapportAnomalieDTO;
import com.backend.backend_pfe.DTO.response.PmRapportMensuelDTO;
import com.backend.backend_pfe.Entity.Affectation;
import com.backend.backend_pfe.Entity.AnomalieV2;
import com.backend.backend_pfe.Entity.Prevision;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.AffectationRepository;
import com.backend.backend_pfe.Repository.AnomalieV2Repository;
import com.backend.backend_pfe.Repository.PrevisionRepository;
import com.backend.backend_pfe.Repository.ProjetRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.TypeAnomalieV2;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PmRapportV2ServiceImpl implements PmRapportV2Service {

    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;
    private final PrevisionRepository previsionRepository;
    private final AffectationRepository affectationRepository;
    private final AnomalieV2Repository anomalieV2Repository;
    private final StaffingCalculService staffingCalculService;

    @Override
    @Transactional(readOnly = true)
    public List<PmRapportMensuelDTO> getRapportsMensuels(Authentication authentication) {
        User chef = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Chef de projet introuvable"));

        List<Projet> projetsChef = projetRepository.findByChefProjet(chef);
        if (projetsChef.isEmpty()) {
            return List.of();
        }

        Map<YearMonth, Set<Projet>> projetsParMois = collectProjetsParMois(projetsChef);
        if (projetsParMois.isEmpty()) {
            return List.of();
        }

        return projetsParMois.entrySet().stream()
                .sorted(Map.Entry.<YearMonth, Set<Projet>>comparingByKey().reversed())
                .map(entry -> buildRapport(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<YearMonth, Set<Projet>> collectProjetsParMois(List<Projet> projetsChef) {
        Map<YearMonth, Set<Projet>> result = new HashMap<>();

        for (Projet projet : projetsChef) {
            for (Prevision prevision : previsionRepository.findByProjet(projet)) {
                if (prevision.getPeriodeDebut() == null || prevision.getPeriodeFin() == null) {
                    continue;
                }
                YearMonth current = YearMonth.from(prevision.getPeriodeDebut());
                YearMonth end = YearMonth.from(prevision.getPeriodeFin());
                while (!current.isAfter(end)) {
                    result.computeIfAbsent(current, key -> new LinkedHashSet<>()).add(projet);
                    current = current.plusMonths(1);
                }
            }
        }

        return result;
    }

    private PmRapportMensuelDTO buildRapport(YearMonth ym, Set<Projet> projets) {
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        Set<Long> collaborateurIds = projets.stream()
                .flatMap(projet -> affectationRepository.findByProjet(projet).stream())
                .filter(a -> overlaps(a, monthStart, monthEnd))
                .map(a -> a.getCollaborateur().getId())
                .collect(Collectors.toSet());

        List<AnomalieV2> anomalies = anomalieV2Repository
                .findByAnneeAndMoisOrderByDateDetectionDesc(ym.getYear(), ym.getMonthValue())
                .stream()
                .filter(a -> a.getCollaborateur() != null)
                .filter(a -> collaborateurIds.contains(a.getCollaborateur().getId()))
                .toList();

        int conflits = countType(anomalies, TypeAnomalieV2.CONFLIT);
        int surcharges = countType(anomalies, TypeAnomalieV2.SURCHARGE);
        int sousCharges = countType(anomalies, TypeAnomalieV2.SOUS_CHARGE);
        int nonStaffes = countType(anomalies, TypeAnomalieV2.NON_STAFFE);

        Set<String> projetsNoms = projets.stream()
                .map(Projet::getNom)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));

        Set<Long> collaborateursAnomalies = anomalies.stream()
                .map(AnomalieV2::getCollaborateur)
                .filter(Objects::nonNull)
                .map(User::getId)
                .collect(Collectors.toSet());

        return PmRapportMensuelDTO.builder()
                .annee(ym.getYear())
                .mois(ym.getMonthValue())
                .libellePeriode(buildLibellePeriode(ym))
                .nombreTotalAnomalies(anomalies.size())
                .nombreConflits(conflits)
                .nombreSurcharges(surcharges)
                .nombreSousCharges(sousCharges)
                .nombreNonStaffes(nonStaffes)
                .nombreCollaborateursConcernes(collaborateursAnomalies.size())
                .nombreProjetsConcernes(projetsNoms.size())
                .projetsConcernes(new ArrayList<>(projetsNoms))
                .allocationMoyenne(calculateAllocationMoyenne(projets, monthStart, monthEnd))
                .statut(anomalies.isEmpty() ? "SANS_ANOMALIE" : "GENERE")
                .anomalies(anomalies.stream().map(this::toAnomalieDTO).toList())
                .build();
    }

    private PmRapportAnomalieDTO toAnomalieDTO(AnomalieV2 anomalie) {
        return PmRapportAnomalieDTO.builder()
                .idAnomalie(anomalie.getId())
                .collaborateur(anomalie.getCollaborateurNom())
                .projetsConcernes(anomalie.getProjetsConcernes())
                .typeAnomalie(anomalie.getTypeAnomalie().name())
                .statutAnomalie(anomalie.getStatut().name())
                .mois(anomalie.getMois())
                .annee(anomalie.getAnnee())
                .capaciteMensuelle(anomalie.getCapaciteMensuelle())
                .joursDemandes(anomalie.getTotalJoursDemandes())
                .tauxCharge(anomalie.getTauxCharge())
                .messageExplicatif(anomalie.getDescription())
                .build();
    }

    private Double calculateAllocationMoyenne(Set<Projet> projets, LocalDate monthStart, LocalDate monthEnd) {
        int capacite = staffingCalculService.getCapaciteMensuelle(
                monthStart.getYear(), monthStart.getMonthValue(), "ma");
        if (capacite <= 0) {
            return null;
        }

        Map<Long, Integer> joursParCollaborateur = new HashMap<>();
        for (Projet projet : projets) {
            for (Affectation affectation : affectationRepository.findByProjet(projet)) {
                if (!overlaps(affectation, monthStart, monthEnd)) {
                    continue;
                }
                LocalDate debut = affectation.getDateDebut().isBefore(monthStart)
                        ? monthStart : affectation.getDateDebut();
                LocalDate fin = affectation.getDateFin().isAfter(monthEnd)
                        ? monthEnd : affectation.getDateFin();
                int jours = staffingCalculService.countJoursOuvrables(debut, fin);
                joursParCollaborateur.merge(affectation.getCollaborateur().getId(), jours, Integer::sum);
            }
        }

        if (joursParCollaborateur.isEmpty()) {
            return null;
        }

        double moyenne = joursParCollaborateur.values().stream()
                .mapToDouble(jours -> (double) jours / capacite * 100)
                .average()
                .orElse(0);
        return Math.round(moyenne * 10.0) / 10.0;
    }

    private int countType(List<AnomalieV2> anomalies, TypeAnomalieV2 type) {
        return (int) anomalies.stream()
                .filter(anomalie -> anomalie.getTypeAnomalie() == type)
                .count();
    }

    private boolean overlaps(Affectation affectation, LocalDate start, LocalDate end) {
        return affectation.getDateDebut() != null
                && affectation.getDateFin() != null
                && !affectation.getDateFin().isBefore(start)
                && !affectation.getDateDebut().isAfter(end);
    }

    private String buildLibellePeriode(YearMonth ym) {
        String mois = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        return mois.substring(0, 1).toUpperCase(Locale.FRENCH) + mois.substring(1) + " " + ym.getYear();
    }
}
