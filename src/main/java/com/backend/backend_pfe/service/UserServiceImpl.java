package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.UpdateProfileRequestDTO;
import com.backend.backend_pfe.DTO.response.UserProfileDTO;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the UserService interface.
 *
 * SOLID — SRP: handles only user-profile related business logic.
 * SOLID — DIP: depends on UserRepository abstraction.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserProfileDTO getMyProfile(Authentication authentication) {
        User user = findUserByAuth(authentication);
        return toDTO(user);
    }

    @Override
    @Transactional
    public UserProfileDTO updateMyProfile(Authentication authentication, UpdateProfileRequestDTO request) {
        User user = findUserByAuth(authentication);

        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setEmail(request.getEmail());
        user.setPoste(request.getPoste());

        User saved = userRepository.save(user);
        return toDTO(saved);
    }

    @Override
    public List<UserProfileDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────

    private User findUserByAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable : " + email));
    }

    private UserProfileDTO toDTO(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .poste(user.getPoste())
                .matricule(user.getMatricule())
                .role(user.getRole().name())
                .tauxStaffing(user.getTauxStaffing())
                .disponible(user.getDisponible())
                .build();
    }
}

