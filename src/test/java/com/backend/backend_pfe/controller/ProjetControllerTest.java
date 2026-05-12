package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.DTO.request.ProjetRequestDTO;
import com.backend.backend_pfe.DTO.response.ProjetResponseDTO;
import com.backend.backend_pfe.config.SecurityConfig;
import com.backend.backend_pfe.enums.StatutProjet;
import com.backend.backend_pfe.exception.BusinessValidationException;
import com.backend.backend_pfe.exception.GlobalExceptionHandler;
import com.backend.backend_pfe.security.JwtAuthenticationFilter;
import com.backend.backend_pfe.security.JwtService;
import com.backend.backend_pfe.service.ProjetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ProjetController using MockMvc.
 *
 * Validates: Requirements 5.1, 5.3, 5.4, 5.5, 5.6, 5.7
 */
@WebMvcTest(ProjetController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ProjetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjetService projetService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/projets avec données valides → HTTP 201")
    @WithMockUser(username = "chef@example.com", roles = "CHEF_PROJET")
    void creerProjet_withValidData_returns201() throws Exception {
        // Arrange
        ProjetRequestDTO request = ProjetRequestDTO.builder()
                .nom("Projet Alpha")
                .description("Description du projet")
                .dateDebut(LocalDate.of(2025, 1, 1))
                .dateFin(LocalDate.of(2025, 6, 30))
                .statut(StatutProjet.PLANIFIE)
                .build();

        ProjetResponseDTO response = ProjetResponseDTO.builder()
                .id(1L)
                .nom("Projet Alpha")
                .description("Description du projet")
                .dateDebut(LocalDate.of(2025, 1, 1))
                .dateFin(LocalDate.of(2025, 6, 30))
                .statut(StatutProjet.PLANIFIE)
                .chefProjetId(10L)
                .chefProjetNomComplet("Dupont Jean")
                .dateCreation(LocalDateTime.of(2025, 1, 1, 10, 0, 0))
                .build();

        when(projetService.creerProjet(any(ProjetRequestDTO.class), any()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/projets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nom").value("Projet Alpha"))
                .andExpect(jsonPath("$.description").value("Description du projet"))
                .andExpect(jsonPath("$.statut").value("PLANIFIE"))
                .andExpect(jsonPath("$.chefProjetId").value(10))
                .andExpect(jsonPath("$.chefProjetNomComplet").value("Dupont Jean"))
                .andExpect(jsonPath("$.dateCreation").exists());

        verify(projetService).creerProjet(any(ProjetRequestDTO.class), any());
    }

    @Test
    @DisplayName("POST /api/projets avec nom vide → HTTP 400")
    @WithMockUser(username = "chef@example.com", roles = "CHEF_PROJET")
    void creerProjet_withEmptyNom_returns400() throws Exception {
        // Arrange
        ProjetRequestDTO request = ProjetRequestDTO.builder()
                .nom("")
                .description("Description")
                .dateDebut(LocalDate.of(2025, 1, 1))
                .dateFin(LocalDate.of(2025, 6, 30))
                .statut(StatutProjet.PLANIFIE)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/projets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.nom").exists());

        verify(projetService, never()).creerProjet(any(), any());
    }

    @Test
    @DisplayName("POST /api/projets avec dateFin < dateDebut → HTTP 400")
    @WithMockUser(username = "chef@example.com", roles = "CHEF_PROJET")
    void creerProjet_withDateFinBeforeDateDebut_returns400() throws Exception {
        // Arrange
        ProjetRequestDTO request = ProjetRequestDTO.builder()
                .nom("Projet Beta")
                .description("Description")
                .dateDebut(LocalDate.of(2025, 6, 1))
                .dateFin(LocalDate.of(2025, 1, 1))
                .statut(StatutProjet.PLANIFIE)
                .build();

        when(projetService.creerProjet(any(ProjetRequestDTO.class), any()))
                .thenThrow(new BusinessValidationException(
                        "La date de fin doit être postérieure à la date de début"));

        // Act & Assert
        mockMvc.perform(post("/api/projets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "La date de fin doit être postérieure à la date de début"));

        verify(projetService).creerProjet(any(ProjetRequestDTO.class), any());
    }

    @Test
    @DisplayName("POST /api/projets sans authentification → HTTP 401")
    void creerProjet_withoutAuthentication_returns401() throws Exception {
        // Arrange
        ProjetRequestDTO request = ProjetRequestDTO.builder()
                .nom("Projet Gamma")
                .description("Description")
                .dateDebut(LocalDate.of(2025, 1, 1))
                .dateFin(LocalDate.of(2025, 6, 30))
                .statut(StatutProjet.PLANIFIE)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/projets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(projetService, never()).creerProjet(any(), any());
    }

    @Test
    @DisplayName("POST /api/projets avec rôle non CHEF_PROJET → HTTP 403")
    @WithMockUser(username = "collab@example.com", roles = "COLLABORATEUR")
    void creerProjet_withNonChefProjetRole_returns403() throws Exception {
        // Arrange
        ProjetRequestDTO request = ProjetRequestDTO.builder()
                .nom("Projet Delta")
                .description("Description")
                .dateDebut(LocalDate.of(2025, 1, 1))
                .dateFin(LocalDate.of(2025, 6, 30))
                .statut(StatutProjet.PLANIFIE)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/projets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(projetService, never()).creerProjet(any(), any());
    }

    @Test
    @DisplayName("POST /api/projets avec JSON malformé → HTTP 400")
    @WithMockUser(username = "chef@example.com", roles = "CHEF_PROJET")
    void creerProjet_withMalformedJson_returns400() throws Exception {
        // Arrange
        String malformedJson = "{ invalid json content }";

        // Act & Assert
        mockMvc.perform(post("/api/projets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Format de requête invalide"));

        verify(projetService, never()).creerProjet(any(), any());
    }
}
