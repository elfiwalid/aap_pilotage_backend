package com.backend.backend_pfe.Repository;


import com.backend.backend_pfe.Entity.Prevision;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.enums.TypePrevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrevisionRepository extends JpaRepository<Prevision, Long> {

    List<Prevision> findByProjet(Projet projet);

    List<Prevision> findByImportePar(User importePar);

    List<Prevision> findByTypePrevision(TypePrevision typePrevision);

    List<Prevision> findByProjetAndActiveTrue(Projet projet);

    List<Prevision> findByProjetOrderByDateImportDesc(Projet projet);

    List<Prevision> findByProjetAndTypePrevisionAndActiveTrue(Projet projet, TypePrevision typePrevision);

    List<Prevision> findByImporteParAndActiveTrue(User importePar);
}