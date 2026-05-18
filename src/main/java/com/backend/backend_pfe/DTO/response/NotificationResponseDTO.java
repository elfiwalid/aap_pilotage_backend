package com.backend.backend_pfe.DTO.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDTO {
    private Long id;
    private String message;
    private LocalDateTime dateNotification;
    private Boolean lu;
}
