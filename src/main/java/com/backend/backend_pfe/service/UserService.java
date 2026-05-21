package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.UserResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface UserService {
    List<UserResponseDTO> getAllCollaborateurs();
    UserResponseDTO getMyProfile(Authentication authentication);
}
