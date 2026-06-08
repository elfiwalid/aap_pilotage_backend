package com.backend.backend_pfe.property;

import com.backend.backend_pfe.DTO.request.ProjetRequestDTO;
import com.backend.backend_pfe.DTO.response.ProjetResponseDTO;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.ProjetRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.Role;
import com.backend.backend_pfe.enums.StatutProjet;
import com.backend.backend_pfe.service.ProjetServiceImpl;
import net.jqwik.api.*;
import org.assertj.core.api.Assertions;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Property 7: Format du nom complet
 *
 * Pour tout utilisateur (nom, prénom) associé comme chef de projet,
 * le champ chefProjetNomComplet du ProjetResponseDTO doit être exactement
 * la concaténation "nom prénom" (nom suivi d'un espace suivi du prénom).
 *
 * **Validates: Requirements 7.2**
 */
@Tag("Feature: project-creation, Property 7: Format nom complet")
class ProjetNomCompletPropertyTest {

    private final ProjetRepository projetRepository = Mockito.mock(ProjetRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final ProjetServiceImpl projetService = new ProjetServiceImpl(projetRepository, userRepository, null, null, null);

    /**
     * Property 7: Pour tout utilisateur avec nom/prénom aléatoires,
     * chefProjetNomComplet doit être exactement "nom prénom".
     *
     * **Validates: Requirements 7.2**
     */
    @Property(tries = 100)
    void chefProjetNomCompletShouldBeNomSpacePrenom(
            @ForAll("randomNom") String nom,
            @ForAll("randomPrenom") String prenom) {

        // Arrange: create a user with random nom/prénom
        String email = "chef@test.com";

        User chefProjet = User.builder()
                .id(1L)
                .nom(nom)
                .prenom(prenom)
                .email(email)
                .password("password")
                .role(Role.CHEF_PROJET)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(chefProjet));

        // Mock save to simulate persistence (set id and dateCreation)
        when(projetRepository.save(any(Projet.class))).thenAnswer(invocation -> {
            Projet projet = invocation.getArgument(0);
            projet.setId(1L);
            projet.setDateCreation(LocalDateTime.now());
            return projet;
        });

        // Create a valid request DTO
        ProjetRequestDTO request = ProjetRequestDTO.builder()
                .nom("Projet Test")
                .description("Description")
                .dateDebut(LocalDate.of(2025, 1, 1))
                .dateFin(LocalDate.of(2025, 12, 31))
                .statut(StatutProjet.PLANIFIE)
                .build();

        // Mock authentication
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);

        // Act
        ProjetResponseDTO response = projetService.creerProjet(request, authentication);

        // Assert: chefProjetNomComplet must be exactly "nom prénom"
        String expectedNomComplet = nom + " " + prenom;
        Assertions.assertThat(response.getChefProjetNomComplet())
                .isEqualTo(expectedNomComplet);
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> randomNom() {
        // Generate non-empty alphabetic strings (1-50 chars) to simulate realistic names
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> randomPrenom() {
        // Generate non-empty alphabetic strings (1-50 chars) to simulate realistic first names
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50);
    }
}
