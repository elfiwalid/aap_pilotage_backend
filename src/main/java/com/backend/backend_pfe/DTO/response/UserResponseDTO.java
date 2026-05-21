package com.backend.backend_pfe.DTO.response;

import com.backend.backend_pfe.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String poste;
    private String matricule;
    private Double tauxStaffing;
    private Boolean disponible;
    private Role role;
}
