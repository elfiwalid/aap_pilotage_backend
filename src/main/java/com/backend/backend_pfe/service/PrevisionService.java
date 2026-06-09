package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.PrevisionResponseDTO;
import com.backend.backend_pfe.DTO.response.PrevisionStatsDTO;
import com.backend.backend_pfe.enums.TypePrevision;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for prevision (forecast) management operations.
 *
 * SOLID — Interface Segregation Principle (ISP):
 *   Only prevision-related operations are defined here.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 *   The controller depends on this abstraction, not on the implementation.
 */
public interface PrevisionService {

    /**
     * Import a new prevision Excel file for a project.
     * Archives any previously active prevision of the same type.
     *
     * @param projetId the ID of the target project
     * @param file the Excel file to import (.xlsx or .xls)
     * @param typePrevision the type of prevision (TRIMESTRIELLE or ANNUELLE)
     * @param periodeDebut the start date of the period selected by the user
     * @param periodeFin the end date of the period selected by the user
     * @param authentication the security context of the authenticated user
     * @return a DTO containing the created prevision details
     */
    PrevisionResponseDTO importerPrevision(Long projetId, MultipartFile file,
            TypePrevision typePrevision, java.time.LocalDate periodeDebut,
            java.time.LocalDate periodeFin, Authentication authentication);

    /**
     * Retrieve the history of all previsions for a project, ordered by import date descending.
     *
     * @param projetId the ID of the target project
     * @param authentication the security context of the authenticated user
     * @return a list of DTOs containing prevision details
     */
    List<PrevisionResponseDTO> getHistorique(Long projetId, Authentication authentication);

    /**
     * Retrieve the currently active prevision for a project.
     *
     * @param projetId the ID of the target project
     * @param authentication the security context of the authenticated user
     * @return an Optional containing the active prevision DTO, or empty if none
     */
    Optional<PrevisionResponseDTO> getPrevisionActive(Long projetId, Authentication authentication);

    /**
     * Download the Excel file of a specific prevision.
     *
     * @param previsionId the ID of the prevision to download
     * @param authentication the security context of the authenticated user
     * @return a ResponseEntity containing the file bytes with appropriate headers
     */
    ResponseEntity<byte[]> telechargerPrevision(Long previsionId, Authentication authentication);

    /**
     * Retrieve statistics for a specific prevision.
     *
     * @param previsionId the ID of the prevision
     * @param authentication the security context of the authenticated user
     * @return a DTO containing the prevision statistics
     */
    PrevisionStatsDTO getStatistiques(Long previsionId, Authentication authentication);

    /**
     * Delete a prevision owned through a project managed by the authenticated Chef de Projet.
     */
    void supprimerPrevision(Long previsionId, Authentication authentication);
}
