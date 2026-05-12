package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.ProjetRequestDTO;
import com.backend.backend_pfe.DTO.response.ProjetResponseDTO;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.ProjetRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.Role;
import com.backend.backend_pfe.enums.StatutProjet;
import com.backend.backend_pfe.exception.BusinessValidationException;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProjetServiceImpl.
 *
 * Validates: Requirements 2.5, 2.6, 3.1, 3.2, 7.2
 */
@ExtendWith(MockitoExtension.class)
class ProjetServiceImplTest {

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProjetServiceImpl projetService;

    private User chefProjet;
    private ProjetRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        chefProjet = User.builder()
                .id(1L)
                .nom("Dupont")
                .prenom("Jean")
                .email("jean.dupont@example.com")
                .password("encoded-password")
                .role(Role.CHEF_PROJET)
                .build();

        validRequest = ProjetRequestDTO.builder()
                .nom("Projet Alpha")
                .description("Description du projet Alpha")
                .dateDebut(LocalDate.of(2025, 1, 1))
                .dateFin(LocalDate.of(2025, 6, 30))
                .statut(StatutProjet.EN_COURS)
                .build();
    }

    @Test
    @DisplayName("Création réussie avec tous les champs valides - retourne ProjetResponseDTO complet")
    void creerProjet_withAllValidFields_returnsCompleteResponse() {
        // Arrange
        when(authentication.getName()).thenReturn("jean.dupont@example.com");
        when(userRepository.findByEmail("jean.dupont@example.com")).thenReturn(Optional.of(chefProjet));
        when(projetRepository.save(any(Projet.class))).thenAnswer(invocation -> {
            Projet projet = invocation.getArgument(0);
            projet.setId(10L);
            projet.setDateCreation(LocalDateTime.of(2025, 1, 1, 10, 0, 0));
            return projet;
        });

        // Act
        ProjetResponseDTO response = projetService.creerProjet(validRequest, authentication);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getNom()).isEqualTo("Projet Alpha");
        assertThat(response.getDescription()).isEqualTo("Description du projet Alpha");
        assertThat(response.getDateDebut()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(response.getDateFin()).isEqualTo(LocalDate.of(2025, 6, 30));
        assertThat(response.getStatut()).isEqualTo(StatutProjet.EN_COURS);
        assertThat(response.getChefProjetId()).isEqualTo(1L);
        assertThat(response.getChefProjetNomComplet()).isEqualTo("Dupont Jean");
        assertThat(response.getDateCreation()).isNotNull();

        verify(projetRepository).save(any(Projet.class));
        verify(userRepository).findByEmail("jean.dupont@example.com");
    }

    @Test
    @DisplayName("Rejet quand dateFin est antérieure à dateDebut - lance BusinessValidationException")
    void creerProjet_withDateFinBeforeDateDebut_throwsBusinessValidationException() {
        // Arrange
        ProjetRequestDTO invalidRequest = ProjetRequestDTO.builder()
                .nom("Projet Beta")
                .description("Description")
                .dateDebut(LocalDate.of(2025, 6, 1))
                .dateFin(LocalDate.of(2025, 1, 1))
                .statut(StatutProjet.PLANIFIE)
                .build();

        when(authentication.getName()).thenReturn("jean.dupont@example.com");
        when(userRepository.findByEmail("jean.dupont@example.com")).thenReturn(Optional.of(chefProjet));

        // Act & Assert
        assertThatThrownBy(() -> projetService.creerProjet(invalidRequest, authentication))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("La date de fin doit être postérieure à la date de début");

        verify(projetRepository, never()).save(any(Projet.class));
    }

    @Test
    @DisplayName("Rejet quand dateFin est égale à dateDebut - lance BusinessValidationException")
    void creerProjet_withDateFinEqualToDateDebut_throwsBusinessValidationException() {
        // Arrange
        LocalDate sameDate = LocalDate.of(2025, 3, 15);
        ProjetRequestDTO invalidRequest = ProjetRequestDTO.builder()
                .nom("Projet Gamma")
                .description("Description")
                .dateDebut(sameDate)
                .dateFin(sameDate)
                .statut(StatutProjet.PLANIFIE)
                .build();

        when(authentication.getName()).thenReturn("jean.dupont@example.com");
        when(userRepository.findByEmail("jean.dupont@example.com")).thenReturn(Optional.of(chefProjet));

        // Act & Assert
        assertThatThrownBy(() -> projetService.creerProjet(invalidRequest, authentication))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("La date de fin doit être postérieure à la date de début");

        verify(projetRepository, never()).save(any(Projet.class));
    }

    @Test
    @DisplayName("Statut par défaut PLANIFIE quand statut est null")
    void creerProjet_withNullStatut_defaultsToPlanifie() {
        // Arrange
        ProjetRequestDTO requestWithoutStatut = ProjetRequestDTO.builder()
                .nom("Projet Delta")
                .description("Description")
                .dateDebut(LocalDate.of(2025, 1, 1))
                .dateFin(LocalDate.of(2025, 12, 31))
                .statut(null)
                .build();

        when(authentication.getName()).thenReturn("jean.dupont@example.com");
        when(userRepository.findByEmail("jean.dupont@example.com")).thenReturn(Optional.of(chefProjet));
        when(projetRepository.save(any(Projet.class))).thenAnswer(invocation -> {
            Projet projet = invocation.getArgument(0);
            projet.setId(20L);
            projet.setDateCreation(LocalDateTime.now());
            return projet;
        });

        // Act
        ProjetResponseDTO response = projetService.creerProjet(requestWithoutStatut, authentication);

        // Assert
        assertThat(response.getStatut()).isEqualTo(StatutProjet.PLANIFIE);
    }

    @Test
    @DisplayName("Rejet quand l'utilisateur JWT est introuvable - lance ResourceNotFoundException")
    void creerProjet_withUnknownJwtUser_throwsResourceNotFoundException() {
        // Arrange
        when(authentication.getName()).thenReturn("inconnu@example.com");
        when(userRepository.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projetService.creerProjet(validRequest, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Chef de projet introuvable");

        verify(projetRepository, never()).save(any(Projet.class));
    }

    @Test
    @DisplayName("chefProjetNomComplet est formaté comme 'nom prénom'")
    void creerProjet_returnsChefProjetNomComplet_asNomEspacePrenom() {
        // Arrange
        User userWithSpecificName = User.builder()
                .id(5L)
                .nom("Martin")
                .prenom("Sophie")
                .email("sophie.martin@example.com")
                .password("encoded-password")
                .role(Role.CHEF_PROJET)
                .build();

        ProjetRequestDTO request = ProjetRequestDTO.builder()
                .nom("Projet Epsilon")
                .description("Test nom complet")
                .dateDebut(LocalDate.of(2025, 2, 1))
                .dateFin(LocalDate.of(2025, 8, 31))
                .statut(StatutProjet.PLANIFIE)
                .build();

        when(authentication.getName()).thenReturn("sophie.martin@example.com");
        when(userRepository.findByEmail("sophie.martin@example.com")).thenReturn(Optional.of(userWithSpecificName));
        when(projetRepository.save(any(Projet.class))).thenAnswer(invocation -> {
            Projet projet = invocation.getArgument(0);
            projet.setId(30L);
            projet.setDateCreation(LocalDateTime.now());
            return projet;
        });

        // Act
        ProjetResponseDTO response = projetService.creerProjet(request, authentication);

        // Assert
        assertThat(response.getChefProjetNomComplet()).isEqualTo("Martin Sophie");
        assertThat(response.getChefProjetId()).isEqualTo(5L);
    }
}
