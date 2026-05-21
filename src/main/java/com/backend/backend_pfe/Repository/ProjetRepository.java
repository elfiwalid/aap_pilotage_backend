package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.enums.StatutProjet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjetRepository extends JpaRepository<Projet, Long> {

    List<Projet> findByChefProjet(User chefProjet);

    List<Projet> findByStatut(StatutProjet statut);

    List<Projet> findByChefProjetAndStatut(User chefProjet, StatutProjet statut);
}