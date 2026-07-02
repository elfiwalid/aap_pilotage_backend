package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.UpdateTacheCollaborateurRequestDTO;
import com.backend.backend_pfe.DTO.response.ImportTachesResponseDTO;
import com.backend.backend_pfe.DTO.response.TacheCollaborateurDTO;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TacheCollaborateurService {

    ImportTachesResponseDTO importerTaches(Long projetId, MultipartFile file, Authentication authentication);

    List<TacheCollaborateurDTO> getTachesCollaborateur(Authentication authentication, Integer annee, Integer mois);

    TacheCollaborateurDTO updateTacheCollaborateur(
            Long tacheId, UpdateTacheCollaborateurRequestDTO request, Authentication authentication);

    List<TacheCollaborateurDTO> getTachesProjet(Long projetId, Authentication authentication);
}
