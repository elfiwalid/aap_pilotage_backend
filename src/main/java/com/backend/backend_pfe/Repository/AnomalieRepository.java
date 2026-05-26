package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.Anomalie;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.enums.StatutAnomalie;
import com.backend.backend_pfe.enums.TypeAnomalie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnomalieRepository extends JpaRepository<Anomalie, Long> {

    List<Anomalie> findByProjet(Projet projet);

    List<Anomalie> findByCollaborateur(User collaborateur);

    List<Anomalie> findByStatut(StatutAnomalie statut);

    List<Anomalie> findByProjetAndStatut(Projet projet, StatutAnomalie statut);

    List<Anomalie> findByCollaborateurAndTypeAnomalieAndProjet(User collaborateur, TypeAnomalie typeAnomalie, Projet projet);
}