package com.backend.backend_pfe.service;

import com.backend.backend_pfe.Entity.Affectation;
import com.backend.backend_pfe.Entity.Anomalie;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.AffectationRepository;
import com.backend.backend_pfe.Repository.AnomalieRepository;
import com.backend.backend_pfe.Repository.PrevisionRepository;
import com.backend.backend_pfe.enums.StatutAnomalie;
import com.backend.backend_pfe.enums.TypeAnomalie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalieDetectionServiceImpl implements AnomalieDetectionService {

    private final AffectationRepository affectationRepository;
    private final AnomalieRepository anomalieRepository;
    private final PrevisionRepository previsionRepository;

    private static final double SEUIL_SURCHARGE = 100.0;
    private static final double SEUIL_SOUS_CHARGE = 50.0;

    @Override
    @Transactional
    public void detecterAnomalies(Projet projet, LocalDate periodeDebut, LocalDate periodeFin) {
        try {
            // 1. Récupérer les collaborateurs du projet importé
            List<Affectation> affectationsProjet = affectationRepository.findByProjet(projet);
            List<User> collaborateurs = affectationsProjet.stream()
                    .map(Affectation::getCollaborateur)
                    .distinct()
                    .toList();

            // 2. Identifier tous les projets impactés (ceux où sont affectés ces collaborateurs)
            Set<Projet> projetsImpactes = new HashSet<>();
            projetsImpactes.add(projet);
            for (User collab : collaborateurs) {
                affectationRepository.findByCollaborateur(collab).stream()
                        .filter(a -> periodsOverlap(a.getDateDebut(), a.getDateFin(),
                                periodeDebut, periodeFin))
                        .map(Affectation::getProjet)
                        .forEach(projetsImpactes::add);
            }

            // 3. Pour CHAQUE projet impacté, détecter les anomalies de ses collaborateurs
            for (Projet projetImpacte : projetsImpactes) {
                List<Affectation> affectationsDuProjet = affectationRepository.findByProjet(projetImpacte);
                List<User> collaborateursDuProjet = affectationsDuProjet.stream()
                        .map(Affectation::getCollaborateur)
                        .distinct()
                        .toList();

                List<Anomalie> nouvellesAnomalies = new ArrayList<>();
                for (User collaborateur : collaborateursDuProjet) {
                    List<Affectation> toutesAffectations = affectationRepository
                            .findByCollaborateur(collaborateur);

                    // Filtrer les affectations chevauchant la période
                    List<Affectation> affectationsPeriode = toutesAffectations.stream()
                            .filter(a -> periodsOverlap(a.getDateDebut(), a.getDateFin(),
                                    periodeDebut, periodeFin))
                            .toList();

                    // Détection avec priorité : SURCHARGE > CONFLIT > SOUS-CHARGE
                    // Une seule anomalie par collaborateur, la plus grave
                    boolean detected = detecterSurcharge(collaborateur, affectationsPeriode,
                            projetImpacte, periodeDebut, periodeFin, nouvellesAnomalies);

                    if (!detected) {
                        detected = detecterConflit(collaborateur, affectationsPeriode,
                                projetImpacte, periodeDebut, periodeFin, nouvellesAnomalies);
                    }

                    if (!detected) {
                        detecterSousCharge(collaborateur, affectationsPeriode,
                                projetImpacte, periodeDebut, periodeFin, nouvellesAnomalies);
                    }
                }

                // 4. Dédupliquer et nettoyer pour ce projet
                nettoyerEtPersister(projetImpacte, nouvellesAnomalies);
            }

        } catch (Exception e) {
            log.error("Erreur lors de la détection d'anomalies pour le projet {}: {}",
                    projet.getId(), e.getMessage(), e);
            // Ne pas bloquer l'import
        }
    }

    private boolean detecterSurcharge(User collaborateur, List<Affectation> affectations,
            Projet projet, LocalDate debut, LocalDate fin, List<Anomalie> result) {
        double tauxCumule = affectations.stream()
                .mapToDouble(a -> a.getTauxAffectation() != null ? a.getTauxAffectation() : 0.0)
                .sum();

        if (tauxCumule > SEUIL_SURCHARGE) {
            // Grouper par projet pour avoir le détail (nom + chef + taux)
            Map<Long, List<Affectation>> parProjet = affectations.stream()
                    .collect(Collectors.groupingBy(a -> a.getProjet().getId()));

            String projetsContribuants = parProjet.values().stream()
                    .map(list -> {
                        Projet p = list.get(0).getProjet();
                        double tauxProjet = list.stream()
                                .mapToDouble(a -> a.getTauxAffectation() != null ?
                                        a.getTauxAffectation() : 0.0)
                                .sum();
                        String chef = p.getChefProjet() != null
                                ? p.getChefProjet().getPrenom() + " " + p.getChefProjet().getNom()
                                : "N/A";
                        return String.format("%s [%.0f%%] - chef: %s",
                                p.getNom(), tauxProjet, chef);
                    })
                    .collect(Collectors.joining(" | "));

            result.add(Anomalie.builder()
                    .titre("Surcharge: " + collaborateur.getPrenom() + " " + collaborateur.getNom())
                    .description(String.format(
                            "Taux cumulé: %.0f%% (seuil: 100%%). Projets: %s. Période: %s à %s",
                            tauxCumule, projetsContribuants, debut, fin))
                    .typeAnomalie(TypeAnomalie.SURCHARGE)
                    .statut(StatutAnomalie.OUVERTE)
                    .dateDetection(LocalDateTime.now())
                    .projet(projet)
                    .collaborateur(collaborateur)
                    .build());
            return true;
        }
        return false;
    }

    private boolean detecterConflit(User collaborateur, List<Affectation> affectations,
            Projet projet, LocalDate debut, LocalDate fin, List<Anomalie> result) {
        // Grouper par projet
        Map<Long, List<Affectation>> parProjet = affectations.stream()
                .collect(Collectors.groupingBy(a -> a.getProjet().getId()));

        if (parProjet.size() >= 2) {
            // Vérifier chevauchement entre projets avec info chef de projet
            String projetsConflictuels = parProjet.entrySet().stream()
                    .map(e -> {
                        Projet p = e.getValue().get(0).getProjet();
                        double tauxProjet = e.getValue().stream()
                                .mapToDouble(a -> a.getTauxAffectation() != null ?
                                        a.getTauxAffectation() : 0.0)
                                .sum();
                        String chef = p.getChefProjet() != null
                                ? p.getChefProjet().getPrenom() + " " + p.getChefProjet().getNom()
                                : "N/A";
                        return String.format("%s [%.0f%%] - chef: %s",
                                p.getNom(), tauxProjet, chef);
                    })
                    .collect(Collectors.joining(" | "));

            result.add(Anomalie.builder()
                    .titre("Conflit: " + collaborateur.getPrenom() + " " + collaborateur.getNom())
                    .description(String.format(
                            "Affecté à %d projets simultanément: %s. Période: %s à %s",
                            parProjet.size(), projetsConflictuels, debut, fin))
                    .typeAnomalie(TypeAnomalie.CONFLIT_AFFECTATION)
                    .statut(StatutAnomalie.OUVERTE)
                    .dateDetection(LocalDateTime.now())
                    .projet(projet)
                    .collaborateur(collaborateur)
                    .build());
            return true;
        }
        return false;
    }

    private boolean detecterSousCharge(User collaborateur, List<Affectation> affectations,
            Projet projet, LocalDate debut, LocalDate fin, List<Anomalie> result) {
        if (affectations.isEmpty()) {
            return false; // Pas d'affectation active = pas de sous-charge à signaler
        }

        double tauxCumule = affectations.stream()
                .mapToDouble(a -> a.getTauxAffectation() != null ? a.getTauxAffectation() : 0.0)
                .sum();

        if (tauxCumule < SEUIL_SOUS_CHARGE) {
            result.add(Anomalie.builder()
                    .titre("Sous-charge: " + collaborateur.getPrenom() + " " + collaborateur.getNom())
                    .description(String.format(
                            "Taux cumulé: %.0f%% (seuil minimum: 50%%). Période: %s à %s",
                            tauxCumule, debut, fin))
                    .typeAnomalie(TypeAnomalie.DISPONIBILITE_INSUFFISANTE)
                    .statut(StatutAnomalie.OUVERTE)
                    .dateDetection(LocalDateTime.now())
                    .projet(projet)
                    .collaborateur(collaborateur)
                    .build());
            return true;
        }
        return false;
    }

    private void nettoyerEtPersister(Projet projet, List<Anomalie> nouvellesAnomalies) {
        // Récupérer les anomalies OUVERTE existantes pour ce projet
        List<Anomalie> existantes = anomalieRepository
                .findByProjetAndStatut(projet, StatutAnomalie.OUVERTE);

        // Identifier les anomalies de ce projet qui ne sont plus détectées → RESOLUE
        for (Anomalie existante : existantes) {
            boolean encoreDetectee = nouvellesAnomalies.stream()
                    .anyMatch(n -> isSameAnomalie(existante, n));
            if (!encoreDetectee) {
                existante.setStatut(StatutAnomalie.RESOLUE);
            }
        }

        // Filtrer les nouvelles anomalies qui n'existent pas déjà POUR CE PROJET
        // (chaque chef de projet a ses propres anomalies liées à ses projets)
        List<Anomalie> vraiementNouvelles = nouvellesAnomalies.stream()
                .filter(n -> existantes.stream().noneMatch(e -> isSameAnomalie(e, n)))
                .toList();

        // Persister
        anomalieRepository.saveAll(existantes); // mises à jour RESOLUE
        anomalieRepository.saveAll(vraiementNouvelles); // nouvelles
    }

    /**
     * Comparaison par (collaborateur, typeAnomalie, projet).
     * Une même anomalie ne doit être créée qu'une seule fois par projet.
     */
    private boolean isSameAnomalie(Anomalie a, Anomalie b) {
        return a.getCollaborateur().getId().equals(b.getCollaborateur().getId())
                && a.getTypeAnomalie() == b.getTypeAnomalie()
                && a.getProjet().getId().equals(b.getProjet().getId());
    }

    private boolean periodsOverlap(LocalDate aDebut, LocalDate aFin,
            LocalDate bDebut, LocalDate bFin) {
        return !aFin.isBefore(bDebut) && !aDebut.isAfter(bFin);
    }
}
