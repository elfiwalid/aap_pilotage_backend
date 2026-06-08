package com.backend.backend_pfe.service;

import com.backend.backend_pfe.Entity.*;
import com.backend.backend_pfe.Repository.*;
import com.backend.backend_pfe.enums.Role;
import com.backend.backend_pfe.enums.StatutAnomalieV2;
import com.backend.backend_pfe.enums.TypeAnomalieV2;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Moteur de détection des anomalies de staffing V2.
 *
 * Logique :
 * 1. Récupérer tous les V2 importés (affectations) pour la période
 * 2. Récupérer les jours ouvrables validés
 * 3. Regrouper par numéro d'employé (matricule)
 * 4. Calculer les jours demandés pour chaque affectation
 * 5. Détecter conflits (chevauchement de dates)
 * 6. Détecter surcharges (total jours > capacité)
 * 7. Détecter sous-charges (total jours < capacité)
 * 8. Détecter non-staffés (aucune affectation)
 * 9. Sauvegarder sans doublons
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalieDetectionV2ServiceImpl implements AnomalieDetectionV2Service {

    private final AffectationRepository affectationRepository;
    private final UserRepository userRepository;
    private final AnomalieV2Repository anomalieV2Repository;
    private final JoursOuvrablesConfigRepository joursOuvrablesConfigRepository;
    private final CalendrierService calendrierService;
    private final ProjetRepository projetRepository;
    private final StaffingCalculService staffingCalculService;
    private final SimulationWhatIfRepository simulationWhatIfRepository;

    @Override
    @Transactional
    public List<AnomalieV2> detecterAnomalies(int annee, int mois, String pays) {
        log.info("Lancement détection anomalies pour {}/{} (pays: {})", mois, annee, pays);

        // 0. Détacher les simulations liées avant de supprimer les anomalies
        simulationWhatIfRepository.nullifyAnomalieByPeriode(annee, mois);
        anomalieV2Repository.deleteByAnneeAndMois(annee, mois);
        anomalieV2Repository.flush();

        // 1. Récupérer la capacité mensuelle (jours ouvrables validés)
        int capaciteMensuelle = staffingCalculService.getCapaciteMensuelle(annee, mois, pays);
        log.info("Capacité mensuelle validée: {} jours", capaciteMensuelle);

        // 2. Récupérer tous les collaborateurs
        List<User> tousCollaborateurs = userRepository.findByRole(Role.COLLABORATEUR);

        // 3. Période du mois
        YearMonth ym = YearMonth.of(annee, mois);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        // 4. Pour chaque collaborateur, récupérer ses affectations chevauchant le mois
        List<AnomalieV2> anomaliesDetectees = new ArrayList<>();

        for (User collab : tousCollaborateurs) {
            List<Affectation> affectations = affectationRepository.findByCollaborateur(collab).stream()
                    .filter(a -> a.getDateDebut() != null && a.getDateFin() != null)
                    .filter(a -> staffingCalculService.overlaps(a.getDateDebut(), a.getDateFin(), monthStart, monthEnd))
                    .toList();

            if (affectations.isEmpty()) {
                // NON_STAFFE
                anomaliesDetectees.add(buildNonStaffe(collab, annee, mois, capaciteMensuelle));
                continue;
            }

            // Calculer les jours ouvrables demandés pour chaque affectation (dans le mois)
            Map<Affectation, Integer> joursParAffectation = new LinkedHashMap<>();
            for (Affectation aff : affectations) {
                LocalDate effectiveDebut = aff.getDateDebut().isBefore(monthStart) ? monthStart : aff.getDateDebut();
                LocalDate effectiveFin = aff.getDateFin().isAfter(monthEnd) ? monthEnd : aff.getDateFin();
                int jours = staffingCalculService.countJoursOuvrables(effectiveDebut, effectiveFin);
                joursParAffectation.put(aff, jours);
            }

            int totalJours = joursParAffectation.values().stream().mapToInt(Integer::intValue).sum();

            // Détecter CONFLIT (chevauchement entre affectations de projets différents)
            AnomalieV2 conflit = detecterConflit(collab, affectations, annee, mois, capaciteMensuelle, totalJours);
            if (conflit != null) {
                anomaliesDetectees.add(conflit);
            }

            // Détecter SURCHARGE ou SOUS_CHARGE
            if (totalJours > capaciteMensuelle) {
                anomaliesDetectees.add(buildSurcharge(collab, affectations, joursParAffectation,
                        annee, mois, capaciteMensuelle, totalJours));
            } else if (totalJours < capaciteMensuelle) {
                anomaliesDetectees.add(buildSousCharge(collab, affectations, joursParAffectation,
                        annee, mois, capaciteMensuelle, totalJours));
            }
        }

        // 5. Sauvegarder (table déjà vidée pour ce mois)
        List<AnomalieV2> saved = anomalieV2Repository.saveAll(anomaliesDetectees);

        log.info("Détection terminée: {} anomalies pour {}/{}", saved.size(), mois, annee);
        return saved;
    }

    @Override
    public List<AnomalieV2> getAnomalies(int annee, int mois) {
        return anomalieV2Repository.findByAnneeAndMoisOrderByDateDetectionDesc(annee, mois);
    }

    @Override
    public List<AnomalieV2> getAnomaliesByType(int annee, int mois, TypeAnomalieV2 type) {
        return anomalieV2Repository.findByAnneeAndMoisAndTypeAnomalie(annee, mois, type);
    }

    @Override
    public AnomalieV2 getAnomalie(Long id) {
        return anomalieV2Repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anomalie introuvable"));
    }

    @Override
    @Transactional
    public void changerStatut(Long id, StatutAnomalieV2 statut) {
        AnomalieV2 anomalie = getAnomalie(id);
        anomalie.setStatut(statut);
        anomalieV2Repository.save(anomalie);
    }

    @Override
    public double getTauxCharge(Long collaborateurId, int annee, int mois) {
        User collab = userRepository.findById(collaborateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborateur introuvable"));

        int capacite = staffingCalculService.getCapaciteMensuelle(annee, mois, "ma");
        if (capacite == 0) return 0;

        YearMonth ym = YearMonth.of(annee, mois);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        int totalJours = affectationRepository.findByCollaborateur(collab).stream()
                .filter(a -> a.getDateDebut() != null && a.getDateFin() != null)
                .filter(a -> staffingCalculService.overlaps(a.getDateDebut(), a.getDateFin(), monthStart, monthEnd))
                .mapToInt(a -> {
                    LocalDate deb = a.getDateDebut().isBefore(monthStart) ? monthStart : a.getDateDebut();
                    LocalDate fin = a.getDateFin().isAfter(monthEnd) ? monthEnd : a.getDateFin();
                    return staffingCalculService.countJoursOuvrables(deb, fin);
                })
                .sum();

        return Math.round((double) totalJours / capacite * 1000.0) / 10.0;
    }

    @Override
    public List<AnomalieV2> getAnomaliesParChef(int annee, int mois, Long chefProjetId) {
        User chef = userRepository.findById(chefProjetId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef de projet introuvable"));

        // Récupérer tous les projets du chef
        List<Projet> projetsChef = projetRepository.findByChefProjet(chef);
        if (projetsChef.isEmpty()) return List.of();

        // Récupérer les collaborateurs affectés à ces projets (qui chevauchent le mois)
        YearMonth ym = YearMonth.of(annee, mois);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        Set<Long> collaborateurIds = projetsChef.stream()
                .flatMap(p -> affectationRepository.findByProjet(p).stream())
                .filter(a -> a.getDateDebut() != null && a.getDateFin() != null)
                .filter(a -> staffingCalculService.overlaps(a.getDateDebut(), a.getDateFin(), monthStart, monthEnd))
                .map(a -> a.getCollaborateur().getId())
                .collect(Collectors.toSet());

        if (collaborateurIds.isEmpty()) return List.of();

        // Filtrer les anomalies : seulement celles dont le collaborateur est dans les projets du chef
        return anomalieV2Repository.findByAnneeAndMoisOrderByDateDetectionDesc(annee, mois).stream()
                .filter(a -> a.getCollaborateur() != null && collaborateurIds.contains(a.getCollaborateur().getId()))
                .toList();
    }

    @Override
    public List<int[]> getPeriodesDisponibles() {
        Set<String> seen = new HashSet<>();
        return anomalieV2Repository.findAll().stream()
                .filter(a -> seen.add(a.getAnnee() + "-" + a.getMois()))
                .map(a -> new int[]{a.getAnnee(), a.getMois()})
                .sorted((a, b) -> a[0] != b[0] ? Integer.compare(b[0], a[0]) : Integer.compare(b[1], a[1]))
                .toList();
    }

    @Override
    public List<int[]> getPeriodesDisponiblesParChef(Long chefProjetId) {
        User chef = userRepository.findById(chefProjetId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef de projet introuvable"));

        List<Projet> projetsChef = projetRepository.findByChefProjet(chef);
        if (projetsChef.isEmpty()) return List.of();

        // Trouver toutes les périodes pour lesquelles ces projets ont des affectations
        Set<String> periodes = new HashSet<>();
        for (Projet p : projetsChef) {
            for (Affectation a : affectationRepository.findByProjet(p)) {
                if (a.getDateDebut() != null) {
                    periodes.add(a.getDateDebut().getYear() + "-" + a.getDateDebut().getMonthValue());
                }
            }
        }

        // Retourner seulement les périodes qui ont des anomalies
        Set<String> seen = new HashSet<>();
        return anomalieV2Repository.findAll().stream()
                .filter(a -> periodes.contains(a.getAnnee() + "-" + a.getMois()))
                .filter(a -> seen.add(a.getAnnee() + "-" + a.getMois()))
                .map(a -> new int[]{a.getAnnee(), a.getMois()})
                .sorted((a, b) -> a[0] != b[0] ? Integer.compare(b[0], a[0]) : Integer.compare(b[1], a[1]))
                .toList();
    }

    // ═══════════════════════════════════════════════════════
    // PRIVATE METHODS
    // ═══════════════════════════════════════════════════════

    /**
     * Détecte un conflit de dates entre affectations d'un même collaborateur.
     */
    private AnomalieV2 detecterConflit(User collab, List<Affectation> affectations,
            int annee, int mois, int capacite, int totalJours) {
        // Trouver les paires d'affectations qui se chevauchent (projets différents)
        List<String> conflits = new ArrayList<>();
        LocalDate conflitDebut = null;
        LocalDate conflitFin = null;
        int joursConflit = 0;

        for (int i = 0; i < affectations.size(); i++) {
            for (int j = i + 1; j < affectations.size(); j++) {
                Affectation a = affectations.get(i);
                Affectation b = affectations.get(j);

                // Conflit seulement entre projets différents
                if (a.getProjet().getId().equals(b.getProjet().getId())) continue;

                if (staffingCalculService.overlaps(a.getDateDebut(), a.getDateFin(), b.getDateDebut(), b.getDateFin())) {
                    LocalDate overlapStart = a.getDateDebut().isAfter(b.getDateDebut()) ? a.getDateDebut() : b.getDateDebut();
                    LocalDate overlapEnd = a.getDateFin().isBefore(b.getDateFin()) ? a.getDateFin() : b.getDateFin();
                    int days = staffingCalculService.countJoursOuvrables(overlapStart, overlapEnd);

                    if (days > 0) {
                        conflits.add(String.format("%s (%s→%s) vs %s (%s→%s) = %d jours",
                                a.getProjet().getNom(), a.getDateDebut(), a.getDateFin(),
                                b.getProjet().getNom(), b.getDateDebut(), b.getDateFin(), days));

                        if (conflitDebut == null || overlapStart.isBefore(conflitDebut)) conflitDebut = overlapStart;
                        if (conflitFin == null || overlapEnd.isAfter(conflitFin)) conflitFin = overlapEnd;
                        joursConflit += days;
                    }
                }
            }
        }

        if (conflits.isEmpty()) return null;

        String projets = affectations.stream()
                .map(a -> a.getProjet().getNom())
                .distinct()
                .collect(Collectors.joining(" | "));

        String desc = String.format("%s est demandé sur plusieurs projets avec des dates qui se chevauchent. " +
                "Conflits: %s. Total %d jours en conflit.",
                collab.getPrenom() + " " + collab.getNom(),
                String.join("; ", conflits), joursConflit);

        return AnomalieV2.builder()
                .typeAnomalie(TypeAnomalieV2.CONFLIT)
                .statut(StatutAnomalieV2.DETECTEE)
                .dateDetection(LocalDateTime.now())
                .collaborateur(collab)
                .numeroEmploye(collab.getMatricule())
                .collaborateurNom(collab.getPrenom() + " " + collab.getNom())
                .annee(annee)
                .mois(mois)
                .capaciteMensuelle(capacite)
                .totalJoursDemandes(totalJours)
                .tauxCharge(capacite > 0 ? Math.round((double) totalJours / capacite * 1000.0) / 10.0 : 0)
                .conflitDateDebut(conflitDebut)
                .conflitDateFin(conflitFin)
                .joursEnConflit(joursConflit)
                .projetsConcernes(projets)
                .description(desc)
                .cleDeduplication(String.format("CONFLIT_%s_%d_%d", collab.getMatricule(), annee, mois))
                .build();
    }

    private AnomalieV2 buildSurcharge(User collab, List<Affectation> affectations,
            Map<Affectation, Integer> joursParAff, int annee, int mois, int capacite, int totalJours) {
        int depassement = totalJours - capacite;
        double taux = capacite > 0 ? Math.round((double) totalJours / capacite * 1000.0) / 10.0 : 0;

        String projets = affectations.stream()
                .map(a -> a.getProjet().getNom() + " (" + joursParAff.get(a) + "j)")
                .collect(Collectors.joining(" | "));

        String desc = String.format("%s est en surcharge: %d jours demandés pour une capacité de %d jours " +
                "(taux: %.1f%%, dépassement: %d jours). Projets: %s",
                collab.getPrenom() + " " + collab.getNom(), totalJours, capacite, taux, depassement, projets);

        return AnomalieV2.builder()
                .typeAnomalie(TypeAnomalieV2.SURCHARGE)
                .statut(StatutAnomalieV2.DETECTEE)
                .dateDetection(LocalDateTime.now())
                .collaborateur(collab)
                .numeroEmploye(collab.getMatricule())
                .collaborateurNom(collab.getPrenom() + " " + collab.getNom())
                .annee(annee)
                .mois(mois)
                .capaciteMensuelle(capacite)
                .totalJoursDemandes(totalJours)
                .joursDepassement(depassement)
                .tauxCharge(taux)
                .projetsConcernes(projets)
                .description(desc)
                .cleDeduplication(String.format("SURCHARGE_%s_%d_%d", collab.getMatricule(), annee, mois))
                .build();
    }

    private AnomalieV2 buildSousCharge(User collab, List<Affectation> affectations,
            Map<Affectation, Integer> joursParAff, int annee, int mois, int capacite, int totalJours) {
        int disponibles = capacite - totalJours;
        double taux = capacite > 0 ? Math.round((double) totalJours / capacite * 1000.0) / 10.0 : 0;

        String projets = affectations.stream()
                .map(a -> a.getProjet().getNom() + " (" + joursParAff.get(a) + "j)")
                .collect(Collectors.joining(" | "));

        String desc = String.format("%s est en sous-charge: %d jours demandés pour une capacité de %d jours " +
                "(taux: %.1f%%, %d jours disponibles). Projets: %s",
                collab.getPrenom() + " " + collab.getNom(), totalJours, capacite, taux, disponibles, projets);

        return AnomalieV2.builder()
                .typeAnomalie(TypeAnomalieV2.SOUS_CHARGE)
                .statut(StatutAnomalieV2.DETECTEE)
                .dateDetection(LocalDateTime.now())
                .collaborateur(collab)
                .numeroEmploye(collab.getMatricule())
                .collaborateurNom(collab.getPrenom() + " " + collab.getNom())
                .annee(annee)
                .mois(mois)
                .capaciteMensuelle(capacite)
                .totalJoursDemandes(totalJours)
                .joursDisponibles(disponibles)
                .tauxCharge(taux)
                .projetsConcernes(projets)
                .description(desc)
                .cleDeduplication(String.format("SOUS_CHARGE_%s_%d_%d", collab.getMatricule(), annee, mois))
                .build();
    }

    private AnomalieV2 buildNonStaffe(User collab, int annee, int mois, int capacite) {
        String desc = String.format("%s n'est affecté à aucun projet pour %02d/%d. " +
                "Capacité disponible: %d jours.",
                collab.getPrenom() + " " + collab.getNom(), mois, annee, capacite);

        return AnomalieV2.builder()
                .typeAnomalie(TypeAnomalieV2.NON_STAFFE)
                .statut(StatutAnomalieV2.DETECTEE)
                .dateDetection(LocalDateTime.now())
                .collaborateur(collab)
                .numeroEmploye(collab.getMatricule())
                .collaborateurNom(collab.getPrenom() + " " + collab.getNom())
                .annee(annee)
                .mois(mois)
                .capaciteMensuelle(capacite)
                .totalJoursDemandes(0)
                .joursDisponibles(capacite)
                .tauxCharge(0)
                .projetsConcernes("")
                .description(desc)
                .cleDeduplication(String.format("NON_STAFFE_%s_%d_%d", collab.getMatricule(), annee, mois))
                .build();
    }
}
