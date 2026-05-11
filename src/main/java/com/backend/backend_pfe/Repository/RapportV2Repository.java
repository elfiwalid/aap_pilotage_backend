package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.RapportV2;
import com.backend.backend_pfe.enums.TypeRapport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RapportV2Repository extends JpaRepository<RapportV2, Long> {

    List<RapportV2> findByProjet(Projet projet);

    List<RapportV2> findByTypeRapport(TypeRapport typeRapport);
}