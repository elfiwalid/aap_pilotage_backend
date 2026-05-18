package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.Anomalie;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.enums.StatutAnomalie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnomalieRepository extends JpaRepository<Anomalie, Long> {

    List<Anomalie> findByProjet(Projet projet);

    List<Anomalie> findByCollaborateur(User collaborateur);
    
    boolean existsByCollaborateurAndStatut(User collaborateur, StatutAnomalie statut);

    List<Anomalie> findByStatut(StatutAnomalie statut);
}