package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.ConversationSimulation;
import com.backend.backend_pfe.Entity.SimulationDecision;
import com.backend.backend_pfe.Entity.SimulationWhatIf;
import com.backend.backend_pfe.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SimulationDecisionRepository extends JpaRepository<SimulationDecision, Long> {

    List<SimulationDecision> findByConversation(ConversationSimulation conversation);

    List<SimulationDecision> findBySimulation(SimulationWhatIf simulation);

    Optional<SimulationDecision> findBySimulationAndChefProjet(SimulationWhatIf simulation, User chefProjet);
}
