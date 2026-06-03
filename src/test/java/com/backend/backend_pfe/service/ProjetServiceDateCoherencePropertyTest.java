package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.ProjetRequestDTO;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.ProjetRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.Role;
import com.backend.backend_pfe.enums.StatutProjet;
import com.backend.backend_pfe.exception.BusinessValidationException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for date coherence validation in ProjetServiceImpl.
 *
 * <p><b>Property 4: Cohérence des dates</b></p>
 * <p>For any pair of dates (dateDebut, dateFin) where dateFin ≤ dateDebut,
 * the system must reject project creation with a BusinessValidationException,
 * and no project must be persisted.</p>
 *
 * <p><b>Validates: Requirements 1.6, 2.5, 4.5</b></p>
 *
 * @see ProjetServiceImpl#creerProjet
 */
@Tag("Feature: project-creation, Property 4: Cohérence des dates")
class ProjetServiceDateCoherencePropertyTest {

    private final ProjetRepository projetRepository = Mockito.mock(ProjetRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final ProjetServiceImpl projetService = new ProjetServiceImpl(projetRepository, userRepository, null);

    /**
     * Property 4: Cohérence des dates
     *
     * For any pair (dateDebut, dateFin) where dateFin is strictly before dateDebut,
     * the service must throw BusinessValidationException and never persist.
     *
     * Validates: Requirements 1.6, 2.5, 4.5
     */
    @Property(tries = 100)
    void dateFinBeforeDateDebut_alwaysThrowsBusinessValidationException(
            @ForAll("dateDebutArbitrary") LocalDate dateDebut,
            @ForAll @IntRange(min = 1, max = 3650) int daysBeforeDebut) {

        // Compute dateFin strictly before dateDebut
        LocalDate dateFin = dateDebut.minusDays(daysBeforeDebut);

        // Setup mocks
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("chef@example.com");

        User chefProjet = User.builder()
                .id(1L)
                .nom("Nom")
                .prenom("Prenom")
                .email("chef@example.com")
                .password("password")
                .role(Role.CHEF_PROJET)
                .build();
        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.of(chefProjet));

        // Build request with invalid dates (dateFin < dateDebut)
        ProjetRequestDTO request = ProjetRequestDTO.builder()
                .nom("Projet Test")
                .description("Description test")
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .statut(StatutProjet.PLANIFIE)
                .build();

        // Assert: BusinessValidationException is thrown
        assertThatThrownBy(() -> projetService.creerProjet(request, authentication))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("La date de fin doit être postérieure à la date de début");

        // Assert: no project is persisted
        verify(projetRepository, never()).save(any());
    }

    /**
     * Property 4: Cohérence des dates (equal dates case)
     *
     * For any date where dateFin equals dateDebut,
     * the service must throw BusinessValidationException and never persist.
     *
     * Validates: Requirements 1.6, 2.5, 4.5
     */
    @Property(tries = 100)
    void dateFinEqualToDateDebut_alwaysThrowsBusinessValidationException(
            @ForAll("dateDebutArbitrary") LocalDate date) {

        // Setup mocks
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("chef@example.com");

        User chefProjet = User.builder()
                .id(1L)
                .nom("Nom")
                .prenom("Prenom")
                .email("chef@example.com")
                .password("password")
                .role(Role.CHEF_PROJET)
                .build();
        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.of(chefProjet));

        // Build request with dateFin == dateDebut
        ProjetRequestDTO request = ProjetRequestDTO.builder()
                .nom("Projet Test")
                .description("Description test")
                .dateDebut(date)
                .dateFin(date)
                .statut(StatutProjet.PLANIFIE)
                .build();

        // Assert: BusinessValidationException is thrown
        assertThatThrownBy(() -> projetService.creerProjet(request, authentication))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("La date de fin doit être postérieure à la date de début");

        // Assert: no project is persisted
        verify(projetRepository, never()).save(any());
    }

    /**
     * Provides arbitrary LocalDate values for dateDebut.
     * Generates dates between 2000-01-01 and 2100-12-31 to cover a wide range.
     */
    @Provide
    Arbitrary<LocalDate> dateDebutArbitrary() {
        return Arbitraries.integers()
                .between(0, 36524) // ~100 years of days
                .map(days -> LocalDate.of(2000, 1, 1).plusDays(days));
    }
}
