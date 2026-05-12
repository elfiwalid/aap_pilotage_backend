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
 * Property 6: Association via contexte JWT
 *
 * Pour tout utilisateur authentifié avec le rôle CHEF_PROJET, le projet créé doit
 * avoir son champ chefProjet associé à cet utilisateur (même id, même email),
 * indépendamment de toute valeur chefProjetId éventuellement présente dans le body
 * de la requête.
 *
 * **Validates: Requirements 3.1, 3.4**
 */
@Tag("Feature: project-creation, Property 6: Association JWT")
class ProjetJwtAssociationPropertyTest {

    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    private final ProjetServiceImpl projetService;

    ProjetJwtAssociationPropertyTest() {
        this.projetRepository = Mockito.mock(ProjetRepository.class);
        this.userRepository = Mockito.mock(UserRepository.class);
        this.projetService = new ProjetServiceImpl(projetRepository, userRepository);
    }

    /**
     * Property 6: Pour tout utilisateur CHEF_PROJET aléatoire authentifié via JWT,
     * le chefProjetId dans la réponse doit correspondre à l'id de cet utilisateur.
     *
     * **Validates: Requirements 3.1, 3.4**
     */
    @Property(tries = 100)
    void chefProjetIdInResponseShouldMatchAuthenticatedUser(
            @ForAll("chefProjetUsers") User chefProjet) {

        // Arrange: Mock Authentication to return the user's email
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(chefProjet.getEmail());

        // Mock UserRepository to return the generated user
        when(userRepository.findByEmail(chefProjet.getEmail()))
                .thenReturn(Optional.of(chefProjet));

        // Mock ProjetRepository.save to simulate persistence (set id and dateCreation)
        when(projetRepository.save(any(Projet.class))).thenAnswer(invocation -> {
            Projet projet = invocation.getArgument(0);
            projet.setId(100L);
            projet.setDateCreation(LocalDateTime.now());
            return projet;
        });

        // Build a valid ProjetRequestDTO
        ProjetRequestDTO request = ProjetRequestDTO.builder()
                .nom("Projet Test")
                .description("Description de test")
                .dateDebut(LocalDate.of(2025, 1, 1))
                .dateFin(LocalDate.of(2025, 12, 31))
                .statut(StatutProjet.PLANIFIE)
                .build();

        // Act
        ProjetResponseDTO response = projetService.creerProjet(request, authentication);

        // Assert: chefProjetId in response matches the authenticated user's id
        Assertions.assertThat(response.getChefProjetId())
                .isNotNull()
                .isEqualTo(chefProjet.getId());
    }

    // --- Generator ---

    @Provide
    Arbitrary<User> chefProjetUsers() {
        Arbitrary<Long> ids = Arbitraries.longs().between(1L, 100_000L);
        Arbitrary<String> emails = Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(15)
                .map(s -> s.toLowerCase() + "@example.com");
        Arbitrary<String> noms = Arbitraries.strings()
                .alpha()
                .ofMinLength(2)
                .ofMaxLength(20);
        Arbitrary<String> prenoms = Arbitraries.strings()
                .alpha()
                .ofMinLength(2)
                .ofMaxLength(20);

        return Combinators.combine(ids, emails, noms, prenoms)
                .as((id, email, nom, prenom) -> {
                    User user = new User();
                    user.setId(id);
                    user.setEmail(email);
                    user.setNom(nom);
                    user.setPrenom(prenom);
                    user.setRole(Role.CHEF_PROJET);
                    user.setPassword("hashed_password");
                    return user;
                });
    }
}
