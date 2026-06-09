package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.ConversationSimulation;
import com.backend.backend_pfe.Entity.MessageConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageConversationRepository extends JpaRepository<MessageConversation, Long> {

    List<MessageConversation> findByConversationOrderByDateEnvoiAsc(ConversationSimulation conversation);
}
