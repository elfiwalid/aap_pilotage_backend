package com.backend.backend_pfe.Repository;


import com.backend.backend_pfe.Entity.Affectation;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AffectationRepository extends JpaRepository<Affectation, Long> {

    List<Affectation> findByProjet(Projet projet);

    List<Affectation> findByCollaborateur(User collaborateur);
}