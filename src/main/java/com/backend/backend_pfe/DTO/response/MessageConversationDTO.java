package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.MessageConversationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageConversationDTO {

    private Long id;
    private Long auteurId;
    private String auteurNomComplet;
    private MessageConversationType type;
    private String contenu;
    private LocalDateTime dateEnvoi;
}
