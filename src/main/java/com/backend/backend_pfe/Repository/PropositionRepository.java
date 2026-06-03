package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.Proposition;
import com.backend.backend_pfe.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropositionRepository extends JpaRepository<Proposition, Long> {

    List<Proposition> findByChefProjetOrderByDatePropositionDesc(User chefProjet);

    List<Proposition> findByChefProjetAndStatut(User chefProjet, String statut);
}
