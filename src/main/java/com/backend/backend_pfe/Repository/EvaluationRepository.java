package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Evaluation entity.
 *
 * SOLID — Interface Segregation: exposes only the query methods
 * needed by the EvaluationService.
 */
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    /** All evaluations received by a collaborateur, newest first. */
    List<Evaluation> findByCollaborateurIdOrderByAnneeDescMoisDesc(Long collaborateurId);

    /** All evaluations given by a chef de projet, newest first. */
    List<Evaluation> findByEvaluateurIdOrderByAnneeDescMoisDesc(Long evaluateurId);

    /** All evaluations given by a chef for a specific month/year. */
    List<Evaluation> findByEvaluateurIdAndAnneeAndMois(Long evaluateurId, Integer annee, Integer mois);

    /** Check if a specific evaluation already exists (prevent duplicates). */
    Optional<Evaluation> findByCollaborateurIdAndEvaluateurIdAndAnneeAndMois(
            Long collaborateurId, Long evaluateurId, Integer annee, Integer mois);
}
