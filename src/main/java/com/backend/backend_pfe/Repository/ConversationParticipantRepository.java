package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.ConversationParticipant;
import com.backend.backend_pfe.Entity.ConversationSimulation;
import com.backend.backend_pfe.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    boolean existsByConversationAndUser(ConversationSimulation conversation, User user);

    Optional<ConversationParticipant> findByConversationAndUser(ConversationSimulation conversation, User user);

    List<ConversationParticipant> findByConversation(ConversationSimulation conversation);
}
