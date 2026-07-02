package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.SimulationRemplacementRequestDTO;
import com.backend.backend_pfe.DTO.request.SimulationDepuisConflitRequestDTO;
import com.backend.backend_pfe.DTO.response.CollaborateurDisponibleConflitDTO;
import com.backend.backend_pfe.DTO.response.SimulationConflitContextDTO;
import com.backend.backend_pfe.DTO.response.SimulationRemplacementResponseDTO;
import com.backend.backend_pfe.DTO.request.SimulationSousChargeRequestDTO;
import com.backend.backend_pfe.DTO.response.SimulationSousChargeResponseDTO;
import com.backend.backend_pfe.Entity.*;
import com.backend.backend_pfe.Repository.*;
import com.backend.backend_pfe.enums.*;
import com.backend.backend_pfe.exception.BusinessValidationException;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class WhatIfSimulationServiceImpl implements WhatIfSimulationService {

    private static final String DEFAULT_COUNTRY = "ma";
    private static final double DEFAULT_TAUX_AFFECTATION = 100.0;

    private final SimulationWhatIfRepository simulationWhatIfRepository;
    private final ScenarioWhatIfRepository scenarioWhatIfRepository;
    private final AnomalieV2Repository anomalieV2Repository;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;
    private final AffectationRepository affectationRepository;
    private final StaffingCalculService staffingCalculService;
    private final AnomalieDetectionV2Service anomalieDetectionV2Service;

    @Override
    public SimulationRemplacementResponseDTO simulerRemplacement(SimulationRemplacementRequestDTO request) {
        validateRequest(request);

        AnomalieV2 anomalie = getAnomalie(request.getAnomalieId());
        User source = getUser(request.getCollaborateurSourceId(), "Collaborateur source introuvable");
        User cible = getUser(request.getCollaborateurCibleId(), "Collaborateur cible introuvable");
        User resourceManager = getUser(request.getResourceManagerId(), "Resource Manager introuvable");
        Projet projet = getProjet(request.getProjetId());

        validateRemplacement(source, cible, request);

        int annee = request.getAnnee() != null ? request.getAnnee() : anomalie.getAnnee();
        int mois = request.getMois() != null ? request.getMois() : anomalie.getMois();
        String pays = request.getPays() != null ? request.getPays() : DEFAULT_COUNTRY;

        int capaciteMensuelle = staffingCalculService.getCapaciteMensuelle(annee, mois, pays);

        double tauxAffectation = request.getTauxAffectation() != null
                ? request.getTauxAffectation()
                : DEFAULT_TAUX_AFFECTATION;

        double joursTransferes = staffingCalculService.calculerJoursDemandesPourPeriode(
                request.getDateDebut(),
                request.getDateFin(),
                tauxAffectation
        );

        double joursSourceAvant = staffingCalculService.calculerJoursDemandes(source, annee, mois);
        double joursCibleAvant = staffingCalculService.calculerJoursDemandes(cible, annee, mois);

        double joursSourceApres = Math.max(0, joursSourceAvant - joursTransferes);
        double joursCibleApres = joursCibleAvant + joursTransferes;

        double tauxSourceAvant = staffingCalculService.calculerTauxCharge(joursSourceAvant, capaciteMensuelle);
        double tauxSourceApres = staffingCalculService.calculerTauxCharge(joursSourceApres, capaciteMensuelle);
        double tauxCibleAvant = staffingCalculService.calculerTauxCharge(joursCibleAvant, capaciteMensuelle);
        double tauxCibleApres = staffingCalculService.calculerTauxCharge(joursCibleApres, capaciteMensuelle);

        boolean nouveauConflit = hasNewConflictForTarget(
                cible.getId(),
                projet.getId(),
                request.getDateDebut(),
                request.getDateFin()
        );

        boolean nouvelleSurcharge = tauxCibleApres > 100;
        boolean conflitCorrige = anomalie.getTypeAnomalie() == TypeAnomalieV2.CONFLIT && !nouveauConflit;
        boolean sousChargeReduite = joursCibleAvant < capaciteMensuelle && joursCibleApres > joursCibleAvant;

        ResultatSimulationWhatIf resultat = determineResult(nouveauConflit, nouvelleSurcharge);
        String commentaire = buildCommentaire(resultat, conflitCorrige, nouvelleSurcharge, nouveauConflit, sousChargeReduite);

        SimulationWhatIf simulation = saveSimulation(
                anomalie,
                resourceManager,
                resultat,
                commentaire
        );

        saveScenario(
                simulation,
                source,
                cible,
                projet,
                request,
                tauxAffectation,
                joursSourceAvant,
                joursSourceApres,
                joursCibleAvant,
                joursCibleApres,
                tauxSourceAvant,
                tauxSourceApres,
                tauxCibleAvant,
                tauxCibleApres,
                conflitCorrige,
                nouvelleSurcharge,
                nouveauConflit,
                sousChargeReduite,
                commentaire
        );

        return buildResponse(
                simulation,
                source,
                cible,
                resultat,
                commentaire,
                joursSourceAvant,
                joursSourceApres,
                joursCibleAvant,
                joursCibleApres,
                tauxSourceAvant,
                tauxSourceApres,
                tauxCibleAvant,
                tauxCibleApres,
                conflitCorrige,
                nouvelleSurcharge,
                nouveauConflit,
                sousChargeReduite
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SimulationConflitContextDTO getConflitContext(Long conflitId) {
        AnomalieV2 conflit = getConflit(conflitId);
        User source = getCollaborateurConflit(conflit);
        LocalDate dateDebut = getConflitDateDebut(conflit);
        LocalDate dateFin = getConflitDateFin(conflit);
        List<Affectation> affectationsConflit = getAffectationsSourceSurConflit(source, dateDebut, dateFin);

        List<SimulationConflitContextDTO.ProjetConflitDTO> projets = affectationsConflit.stream()
                .map(affectation -> SimulationConflitContextDTO.ProjetConflitDTO.builder()
                        .projetId(affectation.getProjet().getId())
                        .projetNom(affectation.getProjet().getNom())
                        .chefProjetNomComplet(buildFullName(affectation.getProjet().getChefProjet()))
                        .dateDebut(affectation.getDateDebut())
                        .dateFin(affectation.getDateFin())
                        .tauxAffectation(affectation.getTauxAffectation())
                        .joursOuvrables(staffingCalculService.countJoursOuvrables(
                                maxDate(affectation.getDateDebut(), dateDebut),
                                minDate(affectation.getDateFin(), dateFin)
                        ))
                        .build())
                .toList();

        return SimulationConflitContextDTO.builder()
                .conflitId(conflit.getId())
                .collaborateurSourceId(source.getId())
                .collaborateurSourceNomComplet(buildFullName(source))
                .matricule(source.getMatricule())
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .annee(conflit.getAnnee())
                .mois(conflit.getMois())
                .tauxCharge(conflit.getTauxCharge())
                .joursEnConflit(conflit.getJoursEnConflit())
                .description(conflit.getDescription())
                .projetsConflit(projets)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollaborateurDisponibleConflitDTO> getCollaborateursDisponiblesPourConflit(Long conflitId) {
        AnomalieV2 conflit = getConflit(conflitId);
        User source = getCollaborateurConflit(conflit);
        LocalDate dateDebut = getConflitDateDebut(conflit);
        LocalDate dateFin = getConflitDateFin(conflit);
        double joursTransferes = staffingCalculService.calculerJoursDemandesPourPeriode(
                dateDebut,
                dateFin,
                DEFAULT_TAUX_AFFECTATION
        );
        int capaciteMensuelle = staffingCalculService.getCapaciteMensuelle(
                conflit.getAnnee(),
                conflit.getMois(),
                DEFAULT_COUNTRY
        );

        List<CollaborateurDisponibleConflitDTO> disponibles = userRepository.findByRole(Role.COLLABORATEUR)
                .stream()
                .filter(candidat -> !candidat.getId().equals(source.getId()))
                .filter(candidat -> !Boolean.FALSE.equals(candidat.getDisponible()))
                .map(candidat -> toCollaborateurDisponible(candidat, conflit, dateDebut, dateFin, joursTransferes, capaciteMensuelle))
                .filter(dto -> dto != null)
                .toList();

        return disponibles;
    }

    @Override
    public SimulationRemplacementResponseDTO simulerDepuisConflit(SimulationDepuisConflitRequestDTO request) {
        AnomalieV2 conflit = getConflit(request.getConflitId());
        User source = getCollaborateurConflit(conflit);
        User cible = getUser(request.getCollaborateurCibleId(), "Collaborateur cible introuvable");
        User resourceManager = getUser(request.getResourceManagerId(), "Resource Manager introuvable");
        LocalDate dateDebut = getConflitDateDebut(conflit);
        LocalDate dateFin = getConflitDateFin(conflit);
        List<Affectation> affectationsConflit = getAffectationsSourceSurConflit(source, dateDebut, dateFin);

        List<CollaborateurDisponibleConflitDTO> candidatsDisponibles =
                getCollaborateursDisponiblesPourConflit(conflit.getId());

        if (candidatsDisponibles.isEmpty()) {
            throw new BusinessValidationException("Aucun collaborateur disponible pour ce conflit");
        }

        boolean candidatDisponible = candidatsDisponibles.stream()
                .anyMatch(candidat -> candidat.getId().equals(request.getCollaborateurCibleId()));

        if (!candidatDisponible) {
            throw new BusinessValidationException("Collaborateur cible non disponible pour cette periode de conflit");
        }

        int capaciteMensuelle = staffingCalculService.getCapaciteMensuelle(
                conflit.getAnnee(),
                conflit.getMois(),
                request.getPays() != null ? request.getPays() : DEFAULT_COUNTRY
        );
        double tauxAffectation = request.getTauxAffectation() != null
                ? request.getTauxAffectation()
                : DEFAULT_TAUX_AFFECTATION;
        double joursTransferes = staffingCalculService.calculerJoursDemandesPourPeriode(
                dateDebut,
                dateFin,
                tauxAffectation
        );
        double joursSourceAvant = staffingCalculService.calculerJoursDemandes(source, conflit.getAnnee(), conflit.getMois());
        double joursCibleAvant = staffingCalculService.calculerJoursDemandes(cible, conflit.getAnnee(), conflit.getMois());
        double joursSourceApres = Math.max(0, joursSourceAvant - joursTransferes);
        double joursCibleApres = joursCibleAvant + joursTransferes;
        double tauxSourceAvant = staffingCalculService.calculerTauxCharge(joursSourceAvant, capaciteMensuelle);
        double tauxSourceApres = staffingCalculService.calculerTauxCharge(joursSourceApres, capaciteMensuelle);
        double tauxCibleAvant = staffingCalculService.calculerTauxCharge(joursCibleAvant, capaciteMensuelle);
        double tauxCibleApres = staffingCalculService.calculerTauxCharge(joursCibleApres, capaciteMensuelle);
        boolean nouvelleSurcharge = tauxCibleApres > 100;
        boolean nouveauConflit = !affectationRepository.findAffectationsChevauchantes(cible.getId(), dateDebut, dateFin).isEmpty();
        boolean conflitCorrige = !nouveauConflit && !nouvelleSurcharge;
        boolean sousChargeReduite = joursCibleAvant < capaciteMensuelle && joursCibleApres > joursCibleAvant;
        ResultatSimulationWhatIf resultat = determineResult(nouveauConflit, nouvelleSurcharge);
        String commentaire = buildCommentaire(resultat, conflitCorrige, nouvelleSurcharge, nouveauConflit, sousChargeReduite);
        List<String> projetsConflit = affectationsConflit.stream()
                .map(affectation -> affectation.getProjet().getNom())
                .distinct()
                .toList();

        SimulationWhatIf simulation = saveSimulation(conflit, resourceManager, resultat, commentaire);
        ScenarioWhatIf scenario = ScenarioWhatIf.builder()
                .simulation(simulation)
                .collaborateurSource(source)
                .collaborateurCible(cible)
                .projet(null)
                .simulationGlobaleConflit(true)
                .projetsConflit(String.join(" | ", projetsConflit))
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .tauxAffectation(tauxAffectation)
                .joursSourceAvant(joursSourceAvant)
                .joursSourceApres(joursSourceApres)
                .joursCibleAvant(joursCibleAvant)
                .joursCibleApres(joursCibleApres)
                .tauxSourceAvant(tauxSourceAvant)
                .tauxSourceApres(tauxSourceApres)
                .tauxCibleAvant(tauxCibleAvant)
                .tauxCibleApres(tauxCibleApres)
                .conflitCorrige(conflitCorrige)
                .nouvelleSurcharge(nouvelleSurcharge)
                .nouveauConflit(nouveauConflit)
                .sousChargeReduite(sousChargeReduite)
                .commentaire(commentaire)
                .build();
        scenarioWhatIfRepository.save(scenario);

        return buildResponse(
                simulation,
                source,
                cible,
                resultat,
                commentaire,
                joursSourceAvant,
                joursSourceApres,
                joursCibleAvant,
                joursCibleApres,
                tauxSourceAvant,
                tauxSourceApres,
                tauxCibleAvant,
                tauxCibleApres,
                conflitCorrige,
                nouvelleSurcharge,
                nouveauConflit,
                sousChargeReduite
        ).toBuilder()
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .projetsConflit(projetsConflit)
                .build();
    }

    @Override
    public void validerSimulation(Long simulationId) {
        SimulationWhatIf simulation = simulationWhatIfRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation introuvable"));

        if (simulation.getStatut() == StatutSimulationWhatIf.VALIDEE) {
            throw new BusinessValidationException("Cette simulation est déjà validée");
        }

        if (simulation.getStatut() == StatutSimulationWhatIf.ANNULEE) {
            throw new BusinessValidationException("Impossible de valider une simulation annulée");
        }

        if (simulation.getResultat() == ResultatSimulationWhatIf.NEGATIF) {
            throw new BusinessValidationException("Impossible de valider une simulation négative");
        }

        ScenarioWhatIf scenario = scenarioWhatIfRepository.findBySimulationId(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Scénario introuvable"));

        if (Boolean.TRUE.equals(scenario.getSimulationGlobaleConflit())) {
            throw new BusinessValidationException(
                    "Cette simulation est une proposition globale de conflit et doit etre traitee via la messagerie"
            );
        }

        if (simulation.getTypeSimulation() == TypeSimulationWhatIf.REMPLACEMENT) {
            appliquerRemplacement(scenario);
        } else if (simulation.getTypeSimulation() == TypeSimulationWhatIf.SOUS_CHARGE) {
            appliquerSousChargeFusion(scenario);
        } else {
            throw new BusinessValidationException("Type de simulation non supporté");
        }

        simulation.setStatut(StatutSimulationWhatIf.VALIDEE);
        simulation.setCommentaire("Simulation validée et appliquée aux affectations.");
        simulationWhatIfRepository.save(simulation);

        if (simulation.getAnomalie() != null) {
            int annee = simulation.getAnomalie().getAnnee();
            int mois = simulation.getAnomalie().getMois();

            anomalieDetectionV2Service.detecterAnomalies(
                    annee,
                    mois,
                    DEFAULT_COUNTRY
            );
        }
    }

    private void appliquerSousChargeFusion(ScenarioWhatIf scenario) {
        Long collaborateurId = scenario.getCollaborateurCible().getId();
        Long projetId = scenario.getProjet().getId();
        LocalDate dateDebut = scenario.getDateDebut();
        LocalDate dateFin = scenario.getDateFin();

        boolean conflitAutreProjet = affectationRepository.findAffectationsChevauchantes(
                        collaborateurId,
                        dateDebut,
                        dateFin
                ).stream()
                .anyMatch(affectation -> !affectation.getProjet().getId().equals(projetId));

        if (conflitAutreProjet) {
            throw new BusinessValidationException(
                    "Le collaborateur est deja affecte a un autre projet sur cette periode"
            );
        }

        List<Affectation> affectationsMemeProjet = affectationRepository.findAffectationsProjetSurPeriode(
                collaborateurId,
                projetId,
                dateDebut,
                dateFin
        );

        if (!affectationsMemeProjet.isEmpty()) {
            throw new BusinessValidationException("Cette affectation existe deja");
        }

        Affectation nouvelleAffectation = Affectation.builder()
                .collaborateur(scenario.getCollaborateurCible())
                .projet(scenario.getProjet())
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .tauxAffectation(scenario.getTauxAffectation())
                .roleDansProjet("Collaborateur")
                .build();

        affectationRepository.save(nouvelleAffectation);
    }

    private void appliquerSousCharge(ScenarioWhatIf scenario) {
        boolean dejaAffecteAuProjet = affectationRepository.findAffectationProjetSurPeriode(
                scenario.getCollaborateurCible().getId(),
                scenario.getProjet().getId(),
                scenario.getDateDebut(),
                scenario.getDateFin()
        ).isPresent();

        if (dejaAffecteAuProjet) {
            throw new BusinessValidationException(
                    "Le collaborateur est déjà affecté à ce projet sur cette période"
            );
        }

        Affectation nouvelleAffectation = Affectation.builder()
                .collaborateur(scenario.getCollaborateurCible())
                .projet(scenario.getProjet())
                .dateDebut(scenario.getDateDebut())
                .dateFin(scenario.getDateFin())
                .tauxAffectation(scenario.getTauxAffectation())
                .roleDansProjet("Collaborateur")
                .build();

        affectationRepository.save(nouvelleAffectation);
    }

    private void appliquerRemplacement(ScenarioWhatIf scenario) {
        Affectation affectationSource = affectationRepository.findAffectationProjetSurPeriode(
                scenario.getCollaborateurSource().getId(),
                scenario.getProjet().getId(),
                scenario.getDateDebut(),
                scenario.getDateFin()
        ).orElseThrow(() -> new ResourceNotFoundException("Affectation source introuvable"));

        affectationRepository.delete(affectationSource);

        Affectation nouvelleAffectation = Affectation.builder()
                .collaborateur(scenario.getCollaborateurCible())
                .projet(scenario.getProjet())
                .dateDebut(scenario.getDateDebut())
                .dateFin(scenario.getDateFin())
                .tauxAffectation(scenario.getTauxAffectation())
                .roleDansProjet("Collaborateur")
                .build();

        affectationRepository.save(nouvelleAffectation);
    }

    @Override
    public void annulerSimulation(Long simulationId) {
        SimulationWhatIf simulation = simulationWhatIfRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation introuvable"));

        if (simulation.getStatut() == StatutSimulationWhatIf.VALIDEE) {
            throw new BusinessValidationException("Impossible d'annuler une simulation déjà validée");
        }

        simulation.setStatut(StatutSimulationWhatIf.ANNULEE);
        simulation.setCommentaire("Simulation annulée par le Resource Manager.");
        simulationWhatIfRepository.save(simulation);
    }

    // ═══════════════════════════════════════════════════════
    // PRIVATE METHODS
    // ═══════════════════════════════════════════════════════

    private void validateRequest(SimulationRemplacementRequestDTO request) {
        if (request.getDateFin().isBefore(request.getDateDebut())) {
            throw new BusinessValidationException("La date de fin doit être après la date de début");
        }

        if (request.getCollaborateurSourceId().equals(request.getCollaborateurCibleId())) {
            throw new BusinessValidationException("Le collaborateur source et le collaborateur cible doivent être différents");
        }
    }

    private void validateRemplacement(User source, User cible, SimulationRemplacementRequestDTO request) {
        if (source.getRole() != Role.COLLABORATEUR) {
            throw new BusinessValidationException("Le collaborateur source doit avoir le rôle COLLABORATEUR");
        }

        if (cible.getRole() != Role.COLLABORATEUR) {
            throw new BusinessValidationException("Le collaborateur cible doit avoir le rôle COLLABORATEUR");
        }

        if (Boolean.FALSE.equals(cible.getDisponible())) {
            throw new BusinessValidationException("Le collaborateur cible n'est pas disponible");
        }

        affectationRepository.findAffectationProjetSurPeriode(
                source.getId(),
                request.getProjetId(),
                request.getDateDebut(),
                request.getDateFin()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Le collaborateur source n'est pas affecté à ce projet sur cette période"
        ));
    }

    private boolean hasNewConflictForTarget(Long collaborateurId,
                                            Long projetId,
                                            LocalDate dateDebut,
                                            LocalDate dateFin) {
        return affectationRepository.findAffectationsChevauchantes(collaborateurId, dateDebut, dateFin)
                .stream()
                .anyMatch(affectation -> !affectation.getProjet().getId().equals(projetId));
    }

    private ResultatSimulationWhatIf determineResult(boolean nouveauConflit, boolean nouvelleSurcharge) {
        if (nouveauConflit || nouvelleSurcharge) {
            return ResultatSimulationWhatIf.NEGATIF;
        }

        return ResultatSimulationWhatIf.POSITIF;
    }

    private String buildCommentaire(ResultatSimulationWhatIf resultat,
                                    boolean conflitCorrige,
                                    boolean nouvelleSurcharge,
                                    boolean nouveauConflit,
                                    boolean sousChargeReduite) {
        if (resultat == ResultatSimulationWhatIf.POSITIF) {
            if (conflitCorrige && sousChargeReduite) {
                return "Simulation positive : le conflit est corrigé et la charge du collaborateur cible est améliorée.";
            }

            if (conflitCorrige) {
                return "Simulation positive : le conflit est corrigé sans créer une nouvelle anomalie.";
            }

            return "Simulation positive : le remplacement ne crée ni nouvelle surcharge ni nouveau conflit.";
        }

        if (nouveauConflit) {
            return "Simulation négative : le collaborateur cible a déjà une affectation sur la même période.";
        }

        if (nouvelleSurcharge) {
            return "Simulation négative : le collaborateur cible devient en surcharge.";
        }

        return "Simulation neutre.";
    }

    private SimulationWhatIf saveSimulation(AnomalieV2 anomalie,
                                            User resourceManager,
                                            ResultatSimulationWhatIf resultat,
                                            String commentaire) {
        SimulationWhatIf simulation = SimulationWhatIf.builder()
                .typeSimulation(TypeSimulationWhatIf.REMPLACEMENT)
                .statut(StatutSimulationWhatIf.TERMINEE)
                .resultat(resultat)
                .commentaire(commentaire)
                .anomalie(anomalie)
                .resourceManager(resourceManager)
                .build();

        return simulationWhatIfRepository.save(simulation);
    }

    private void saveScenario(SimulationWhatIf simulation,
                              User source,
                              User cible,
                              Projet projet,
                              SimulationRemplacementRequestDTO request,
                              double tauxAffectation,
                              double joursSourceAvant,
                              double joursSourceApres,
                              double joursCibleAvant,
                              double joursCibleApres,
                              double tauxSourceAvant,
                              double tauxSourceApres,
                              double tauxCibleAvant,
                              double tauxCibleApres,
                              boolean conflitCorrige,
                              boolean nouvelleSurcharge,
                              boolean nouveauConflit,
                              boolean sousChargeReduite,
                              String commentaire) {
        ScenarioWhatIf scenario = ScenarioWhatIf.builder()
                .simulation(simulation)
                .collaborateurSource(source)
                .collaborateurCible(cible)
                .projet(projet)
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .tauxAffectation(tauxAffectation)
                .joursSourceAvant(joursSourceAvant)
                .joursSourceApres(joursSourceApres)
                .joursCibleAvant(joursCibleAvant)
                .joursCibleApres(joursCibleApres)
                .tauxSourceAvant(tauxSourceAvant)
                .tauxSourceApres(tauxSourceApres)
                .tauxCibleAvant(tauxCibleAvant)
                .tauxCibleApres(tauxCibleApres)
                .conflitCorrige(conflitCorrige)
                .nouvelleSurcharge(nouvelleSurcharge)
                .nouveauConflit(nouveauConflit)
                .sousChargeReduite(sousChargeReduite)
                .commentaire(commentaire)
                .build();

        scenarioWhatIfRepository.save(scenario);
    }

    private SimulationRemplacementResponseDTO buildResponse(SimulationWhatIf simulation,
                                                            User source,
                                                            User cible,
                                                            ResultatSimulationWhatIf resultat,
                                                            String commentaire,
                                                            double joursSourceAvant,
                                                            double joursSourceApres,
                                                            double joursCibleAvant,
                                                            double joursCibleApres,
                                                            double tauxSourceAvant,
                                                            double tauxSourceApres,
                                                            double tauxCibleAvant,
                                                            double tauxCibleApres,
                                                            boolean conflitCorrige,
                                                            boolean nouvelleSurcharge,
                                                            boolean nouveauConflit,
                                                            boolean sousChargeReduite) {
        return SimulationRemplacementResponseDTO.builder()
                .simulationId(simulation.getId())
                .typeSimulation(TypeSimulationWhatIf.REMPLACEMENT)
                .resultat(resultat)
                .commentaire(commentaire)

                .collaborateurSource(buildFullName(source))
                .joursSourceAvant(joursSourceAvant)
                .joursSourceApres(joursSourceApres)
                .tauxSourceAvant(tauxSourceAvant)
                .tauxSourceApres(tauxSourceApres)
                .etatSourceApres(staffingCalculService.determinerEtat(tauxSourceApres))

                .collaborateurCible(buildFullName(cible))
                .joursCibleAvant(joursCibleAvant)
                .joursCibleApres(joursCibleApres)
                .tauxCibleAvant(tauxCibleAvant)
                .tauxCibleApres(tauxCibleApres)
                .etatCibleApres(staffingCalculService.determinerEtat(tauxCibleApres))

                .conflitCorrige(conflitCorrige)
                .nouvelleSurcharge(nouvelleSurcharge)
                .nouveauConflit(nouveauConflit)
                .sousChargeReduite(sousChargeReduite)
                .build();
    }

    @Override
    public SimulationSousChargeResponseDTO simulerSousCharge(SimulationSousChargeRequestDTO request) {
        validateSousChargeRequest(request);

        AnomalieV2 anomalie = getAnomalie(request.getAnomalieId());

        User cible = getUser(
                request.getCollaborateurCibleId(),
                "Collaborateur cible introuvable"
        );

        User resourceManager = getUser(
                request.getResourceManagerId(),
                "Resource Manager introuvable"
        );

        Projet projet = getProjet(request.getProjetId());

        validateSousChargeCollaborateur(cible);

        int annee = request.getAnnee() != null ? request.getAnnee() : anomalie.getAnnee();
        int mois = request.getMois() != null ? request.getMois() : anomalie.getMois();
        String pays = request.getPays() != null ? request.getPays() : DEFAULT_COUNTRY;

        int capaciteMensuelle = staffingCalculService.getCapaciteMensuelle(
                annee,
                mois,
                pays
        );

        double tauxAffectation = request.getTauxAffectation() != null
                ? request.getTauxAffectation()
                : DEFAULT_TAUX_AFFECTATION;

        double joursAjoutes = staffingCalculService.calculerJoursDemandesPourPeriode(
                request.getDateDebut(),
                request.getDateFin(),
                tauxAffectation
        );

        double joursCibleAvant = staffingCalculService.calculerJoursDemandes(
                cible,
                annee,
                mois
        );

        double joursCibleApres = joursCibleAvant + joursAjoutes;

        double tauxCibleAvant = staffingCalculService.calculerTauxCharge(
                joursCibleAvant,
                capaciteMensuelle
        );

        double tauxCibleApres = staffingCalculService.calculerTauxCharge(
                joursCibleApres,
                capaciteMensuelle
        );

        boolean affectationMemeProjetExistante = !affectationRepository.findAffectationsProjetSurPeriode(
                cible.getId(),
                projet.getId(),
                request.getDateDebut(),
                request.getDateFin()
        ).isEmpty();

        boolean nouveauConflit = affectationMemeProjetExistante || hasNewConflictForTarget(
                cible.getId(),
                projet.getId(),
                request.getDateDebut(),
                request.getDateFin()
        );

        boolean nouvelleSurcharge = tauxCibleApres > 100;

        boolean sousChargeReduite =
                joursCibleAvant < capaciteMensuelle
                        && joursCibleApres > joursCibleAvant
                        && joursCibleApres <= capaciteMensuelle;

        ResultatSimulationWhatIf resultat = determineResult(
                nouveauConflit,
                nouvelleSurcharge
        );

        String commentaire = buildSousChargeCommentaire(
                resultat,
                sousChargeReduite,
                nouvelleSurcharge,
                nouveauConflit,
                affectationMemeProjetExistante
        );

        SimulationWhatIf simulation = SimulationWhatIf.builder()
                .typeSimulation(TypeSimulationWhatIf.SOUS_CHARGE)
                .statut(StatutSimulationWhatIf.TERMINEE)
                .resultat(resultat)
                .commentaire(commentaire)
                .anomalie(anomalie)
                .resourceManager(resourceManager)
                .build();

        simulationWhatIfRepository.save(simulation);

        ScenarioWhatIf scenario = ScenarioWhatIf.builder()
                .simulation(simulation)

                .collaborateurSource(null)
                .collaborateurCible(cible)
                .projet(projet)

                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .tauxAffectation(tauxAffectation)

                .joursSourceAvant(null)
                .joursSourceApres(null)
                .tauxSourceAvant(null)
                .tauxSourceApres(null)

                .joursCibleAvant(joursCibleAvant)
                .joursCibleApres(joursCibleApres)
                .tauxCibleAvant(tauxCibleAvant)
                .tauxCibleApres(tauxCibleApres)

                .conflitCorrige(false)
                .nouvelleSurcharge(nouvelleSurcharge)
                .nouveauConflit(nouveauConflit)
                .sousChargeReduite(sousChargeReduite)

                .commentaire(commentaire)
                .build();

        scenarioWhatIfRepository.save(scenario);

        return SimulationSousChargeResponseDTO.builder()
                .simulationId(simulation.getId())
                .typeSimulation(TypeSimulationWhatIf.SOUS_CHARGE)
                .resultat(resultat)
                .commentaire(commentaire)

                .collaborateurCible(buildFullName(cible))

                .joursCibleAvant(joursCibleAvant)
                .joursCibleApres(joursCibleApres)

                .tauxCibleAvant(tauxCibleAvant)
                .tauxCibleApres(tauxCibleApres)

                .etatCibleAvant(staffingCalculService.determinerEtat(tauxCibleAvant))
                .etatCibleApres(staffingCalculService.determinerEtat(tauxCibleApres))

                .sousChargeReduite(sousChargeReduite)
                .nouvelleSurcharge(nouvelleSurcharge)
                .nouveauConflit(nouveauConflit)
                .build();
    }

    private void validateSousChargeRequest(SimulationSousChargeRequestDTO request) {
        if (request.getDateFin().isBefore(request.getDateDebut())) {
            throw new BusinessValidationException(
                    "La date de fin doit être après la date de début"
            );
        }
    }

    private void validateSousChargeCollaborateur(User cible) {
        if (cible.getRole() != Role.COLLABORATEUR) {
            throw new BusinessValidationException(
                    "Le collaborateur cible doit avoir le rôle COLLABORATEUR"
            );
        }

        if (Boolean.FALSE.equals(cible.getDisponible())) {
            throw new BusinessValidationException(
                    "Le collaborateur cible n’est pas disponible"
            );
        }
    }

    private String buildSousChargeCommentaire(
            ResultatSimulationWhatIf resultat,
            boolean sousChargeReduite,
            boolean nouvelleSurcharge,
            boolean nouveauConflit,
            boolean affectationMemeProjetExistante
    ) {
        if (affectationMemeProjetExistante) {
            return "Simulation non applicable : cette affectation existe deja sur le meme projet.";
        }

        if (resultat == ResultatSimulationWhatIf.POSITIF) {
            if (sousChargeReduite) {
                return "Simulation positive : la sous-charge du collaborateur est réduite sans créer de nouvelle anomalie.";
            }

            return "Simulation positive : l’affectation ne crée ni conflit ni surcharge.";
        }

        if (nouveauConflit) {
            return "Simulation négative : le collaborateur cible a déjà une affectation sur la même période.";
        }

        if (nouvelleSurcharge) {
            return "Simulation négative : cette affectation rend le collaborateur cible en surcharge.";
        }

        return "Simulation neutre.";
    }

    private AnomalieV2 getAnomalie(Long id) {
        return anomalieV2Repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anomalie introuvable"));
    }

    private User getUser(Long id, String message) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(message));
    }

    private Projet getProjet(Long id) {
        return projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
    }

    private AnomalieV2 getConflit(Long id) {
        AnomalieV2 conflit = getAnomalie(id);

        if (conflit.getTypeAnomalie() != TypeAnomalieV2.CONFLIT) {
            throw new BusinessValidationException("L'anomalie selectionnee n'est pas un conflit");
        }

        if (conflit.getConflitDateDebut() == null || conflit.getConflitDateFin() == null) {
            throw new BusinessValidationException("La periode du conflit est introuvable");
        }

        return conflit;
    }

    private User getCollaborateurConflit(AnomalieV2 conflit) {
        if (conflit.getCollaborateur() != null) {
            return conflit.getCollaborateur();
        }

        if (conflit.getNumeroEmploye() != null && !conflit.getNumeroEmploye().isBlank()) {
            return userRepository.findByMatricule(conflit.getNumeroEmploye())
                    .orElseThrow(() -> new ResourceNotFoundException("Collaborateur source introuvable"));
        }

        throw new ResourceNotFoundException("Collaborateur source introuvable");
    }

    private LocalDate getConflitDateDebut(AnomalieV2 conflit) {
        return conflit.getConflitDateDebut();
    }

    private LocalDate getConflitDateFin(AnomalieV2 conflit) {
        return conflit.getConflitDateFin();
    }

    private List<Affectation> getAffectationsSourceSurConflit(User source, LocalDate dateDebut, LocalDate dateFin) {
        Map<Long, Affectation> parProjet = new LinkedHashMap<>();

        affectationRepository.findAffectationsChevauchantes(source.getId(), dateDebut, dateFin)
                .stream()
                .filter(affectation -> affectation.getProjet() != null)
                .forEach(affectation -> parProjet.putIfAbsent(affectation.getProjet().getId(), affectation));

        if (parProjet.size() < 2) {
            throw new BusinessValidationException("Le conflit ne contient pas au moins deux projets chevauchants");
        }

        return parProjet.values().stream().toList();
    }

    private CollaborateurDisponibleConflitDTO toCollaborateurDisponible(User candidat,
                                                                        AnomalieV2 conflit,
                                                                        LocalDate dateDebut,
                                                                        LocalDate dateFin,
                                                                        double joursTransferes,
                                                                        int capaciteMensuelle) {
        List<Affectation> chevauchements = affectationRepository.findAffectationsChevauchantes(
                candidat.getId(),
                dateDebut,
                dateFin
        );

        if (!chevauchements.isEmpty()) {
            return null;
        }

        double joursAvant = staffingCalculService.calculerJoursDemandes(candidat, conflit.getAnnee(), conflit.getMois());
        double tauxAvant = staffingCalculService.calculerTauxCharge(joursAvant, capaciteMensuelle);

        if (tauxAvant >= 100) {
            return null;
        }

        double joursApres = joursAvant + joursTransferes;
        double tauxApres = staffingCalculService.calculerTauxCharge(joursApres, capaciteMensuelle);

        if (tauxApres > 100) {
            return null;
        }

        return CollaborateurDisponibleConflitDTO.builder()
                .id(candidat.getId())
                .nom(candidat.getNom())
                .prenom(candidat.getPrenom())
                .email(candidat.getEmail())
                .poste(candidat.getPoste())
                .matricule(candidat.getMatricule())
                .tauxUtilisation(tauxAvant)
                .tauxApresSimulation(tauxApres)
                .disponibilite(Math.max(0, 100 - tauxAvant))
                .joursDisponibles(Math.max(0, capaciteMensuelle - joursAvant))
                .build();
    }

    private LocalDate maxDate(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private LocalDate minDate(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private String buildFullName(User user) {
        if (user == null) {
            return null;
        }
        return user.getPrenom() + " " + user.getNom();
    }


}
