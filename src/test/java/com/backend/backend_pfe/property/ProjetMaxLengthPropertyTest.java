package com.backend.backend_pfe.property;

import com.backend.backend_pfe.DTO.request.ProjetRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.*;
import org.assertj.core.api.Assertions;

import java.time.LocalDate;
import java.util.Set;

/**
 * Property 3: Rejet des champs dépassant la longueur maximale
 *
 * Pour toute chaîne de longueur supérieure à 255 caractères soumise comme nom,
 * ou supérieure à 1500 caractères soumise comme description, le système doit
 * rejeter la requête avec une erreur de validation.
 *
 * Validates: Requirements 2.2, 2.8
 */
@Tag("Feature: project-creation, Property 3: Rejet longueur maximale")
class ProjetMaxLengthPropertyTest {

    private final Validator validator;

    ProjetMaxLengthPropertyTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    /**
     * Property 3a: Tout nom dépassant 255 caractères doit être rejeté par la validation.
     *
     * **Validates: Requirements 2.2**
     */
    @Property(tries = 100)
    void nomExceedingMaxLengthShouldBeRejected(
            @ForAll("nomTooLong") String tooLongNom) {

        ProjetRequestDTO dto = ProjetRequestDTO.builder()
                .nom(tooLongNom)
                .description("Description valide")
                .dateDebut(LocalDate.of(2025, 1, 1))
                .dateFin(LocalDate.of(2025, 12, 31))
                .build();

        Set<ConstraintViolation<ProjetRequestDTO>> violations = validator.validate(dto);

        Assertions.assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("nom")
                        && v.getMessage().contains("255"));
    }

    /**
     * Property 3b: Toute description dépassant 1500 caractères doit être rejetée par la validation.
     *
     * **Validates: Requirements 2.8**
     */
    @Property(tries = 100)
    void descriptionExceedingMaxLengthShouldBeRejected(
            @ForAll("descriptionTooLong") String tooLongDescription) {

        ProjetRequestDTO dto = ProjetRequestDTO.builder()
                .nom("Projet valide")
                .description(tooLongDescription)
                .dateDebut(LocalDate.of(2025, 1, 1))
                .dateFin(LocalDate.of(2025, 12, 31))
                .build();

        Set<ConstraintViolation<ProjetRequestDTO>> violations = validator.validate(dto);

        Assertions.assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("description")
                        && v.getMessage().contains("1500"));
    }

    /**
     * Property 3c: Quand nom ET description dépassent les limites, les deux violations sont détectées.
     *
     * **Validates: Requirements 2.2, 2.8**
     */
    @Property(tries = 100)
    void bothFieldsExceedingMaxLengthShouldBothBeRejected(
            @ForAll("nomTooLong") String tooLongNom,
            @ForAll("descriptionTooLong") String tooLongDescription) {

        ProjetRequestDTO dto = ProjetRequestDTO.builder()
                .nom(tooLongNom)
                .description(tooLongDescription)
                .dateDebut(LocalDate.of(2025, 1, 1))
                .dateFin(LocalDate.of(2025, 12, 31))
                .build();

        Set<ConstraintViolation<ProjetRequestDTO>> violations = validator.validate(dto);

        Assertions.assertThat(violations)
                .hasSizeGreaterThanOrEqualTo(2);

        Assertions.assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("nom"));
        Assertions.assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> nomTooLong() {
        // Generate strings with length between 256 and 500 characters
        return Arbitraries.integers().between(256, 500)
                .flatMap(length -> Arbitraries.strings()
                        .alpha()
                        .ofMinLength(length)
                        .ofMaxLength(length));
    }

    @Provide
    Arbitrary<String> descriptionTooLong() {
        // Generate strings with length between 1501 and 3000 characters
        return Arbitraries.integers().between(1501, 3000)
                .flatMap(length -> Arbitraries.strings()
                        .alpha()
                        .ofMinLength(length)
                        .ofMaxLength(length));
    }
}
