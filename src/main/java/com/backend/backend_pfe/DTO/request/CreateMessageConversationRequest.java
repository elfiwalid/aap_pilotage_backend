package com.backend.backend_pfe.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMessageConversationRequest {

    @NotBlank
    private String contenu;
}
