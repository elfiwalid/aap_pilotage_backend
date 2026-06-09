package com.backend.backend_pfe.Entity;

import com.backend.backend_pfe.enums.MessageConversationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages_conversation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationSimulation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id")
    private User auteur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageConversationType type;

    @Column(nullable = false, length = 4000)
    private String contenu;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateEnvoi;

    @PrePersist
    protected void onCreate() {
        if (dateEnvoi == null) {
            dateEnvoi = LocalDateTime.now();
        }
        if (type == null) {
            type = MessageConversationType.USER;
        }
    }
}
