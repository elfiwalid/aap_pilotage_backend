package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.ConversationSimulation;
import com.backend.backend_pfe.Entity.SimulationWhatIf;
import com.backend.backend_pfe.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationSimulationRepository extends JpaRepository<ConversationSimulation, Long> {

    Optional<ConversationSimulation> findBySimulation(SimulationWhatIf simulation);

    @Query("""
        SELECT DISTINCT c FROM ConversationSimulation c
        JOIN ConversationParticipant p ON p.conversation = c
        WHERE p.user = :user
        ORDER BY c.dateCreation DESC
    """)
    List<ConversationSimulation> findByParticipant(@Param("user") User user);
}
