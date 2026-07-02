package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.ProjetRequestDTO;
import com.backend.backend_pfe.DTO.response.ProjetResponseDTO;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.AffectationRepository;
import com.backend.backend_pfe.Repository.AffectationTacheCollaborateurRepository;
import com.backend.backend_pfe.Repository.AnomalieV2Repository;
import com.backend.backend_pfe.Repository.PrevisionRepository;
import com.backend.backend_pfe.Repository.ProjetRepository;
import com.backend.backend_pfe.Repository.SimulationWhatIfRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.Role;
import com.backend.backend_pfe.enums.StatutProjet;
import com.backend.backend_pfe.service.NotificationService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Property-based test for Property 5: Statut par défaut PLANIFIE.
 *
 * For any valid ProjetRequestDTO with statut = null, the created project
 * must have statut = PLANIFIE in the response.
 *
 * Validates: Requirements 2.6, 4.6
 *
 * @Tag("Feature: project-creation, Property 5: Statut par défaut")
 */
@Tag("Feature: project-creation, Property 5: Statut par défaut")
class ProjetServiceStatutDefautPropertyTest {

    private final ProjetRepository projetRepository = Mockito.mock(ProjetRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final ProjetServiceImpl projetService = new ProjetServiceImpl(
            projetRepository,
            userRepository,
            Mockito.mock(NotificationService.class),
            Mockito.mock(AffectationRepository.class),
            Mockito.mock(AnomalieV2Repository.class),
            Mockito.mock(PrevisionRepository.class),
            Mockito.mock(AffectationTacheCollaborateurRepository.class),
            Mockito.mock(SimulationWhatIfRepository.class));

    /**
     * Property 5: Statut par défaut PLANIFIE
     *
     * For all valid ProjetRequestDTO with statut = null,
     * the response must have statut = PLANIFIE.
     *
     * Validates: Requirements 2.6, 4.6
     */
    @Property(tries = 100)
    void statutShouldDefaultToPlanifieWhenNull(
            @ForAll("validNom") String nom,
            @ForAll("validDescription") String description,
            @ForAll("validDatePair") LocalDate[] dates,
            @ForAll("validUser") User user
    ) {
        // Arrange: build a valid DTO with statut = null
        ProjetRequestDTO request = ProjetRequestDTO.builder()
                .nom(nom)
                .description(description)
                .dateDebut(dates[0])
                .dateFin(dates[1])
                .statut(null) // explicitly null to test default behavior
                .build();

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(projetRepository.save(any(Projet.class))).thenAnswer(invocation -> {
            Projet projet = invocation.getArgument(0);
            projet.setId(1L);
            projet.setDateCreation(LocalDateTime.now());
            return projet;
        });

        // Act
        ProjetResponseDTO response = projetService.creerProjet(request, authentication);

        // Assert: statut must be PLANIFIE
        assertThat(response.getStatut()).isEqualTo(StatutProjet.PLANIFIE);
    }

    @Provide
    Arbitrary<String> validNom() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(255)
                .alpha()
                .filter(s -> !s.isBlank());
    }

    @Provide
    Arbitrary<String> validDescription() {
        return Arbitraries.strings()
                .ofMinLength(0)
                .ofMaxLength(1500)
                .alpha()
                .injectNull(0.2);
    }

    @Provide
    Arbitrary<LocalDate[]> validDatePair() {
        return Arbitraries.integers().between(2020, 2030).flatMap(year ->
                Arbitraries.integers().between(1, 12).flatMap(month ->
                        Arbitraries.integers().between(1, 28).flatMap(day -> {
                            LocalDate dateDebut = LocalDate.of(year, month, day);
                            return Arbitraries.integers().between(1, 365).map(daysToAdd -> {
                                LocalDate dateFin = dateDebut.plusDays(daysToAdd);
                                return new LocalDate[]{dateDebut, dateFin};
                            });
                        })
                )
        );
    }

    @Provide
    Arbitrary<User> validUser() {
        Arbitrary<String> noms = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(30);
        Arbitrary<String> prenoms = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(30);
        Arbitrary<Long> ids = Arbitraries.longs().between(1L, 10000L);

        return Combinators.combine(ids, noms, prenoms).as((id, nom, prenom) -> {
            String email = nom.toLowerCase() + "." + prenom.toLowerCase() + "@example.com";
            return User.builder()
                    .id(id)
                    .nom(nom)
                    .prenom(prenom)
                    .email(email)
                    .password("encoded-password")
                    .role(Role.CHEF_PROJET)
                    .build();
        });
    }
}
