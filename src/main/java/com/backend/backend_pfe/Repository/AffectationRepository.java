package com.backend.backend_pfe.Repository;


import com.backend.backend_pfe.Entity.Affectation;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AffectationRepository extends JpaRepository<Affectation, Long> {

    List<Affectation> findByProjet(Projet projet);

    List<Affectation> findByCollaborateur(User collaborateur);

    @Query("""
        SELECT a FROM Affectation a
        WHERE a.collaborateur.id = :collaborateurId
        AND a.dateDebut <= :dateFin
        AND a.dateFin >= :dateDebut
    """)
    List<Affectation> findAffectationsChevauchantes(
            @Param("collaborateurId") Long collaborateurId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    @Query("""
        SELECT a FROM Affectation a
        WHERE a.collaborateur.id = :collaborateurId
        AND a.projet.id = :projetId
        AND a.dateDebut <= :dateFin
        AND a.dateFin >= :dateDebut
    """)
    Optional<Affectation> findAffectationProjetSurPeriode(
            @Param("collaborateurId") Long collaborateurId,
            @Param("projetId") Long projetId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    @Query("""
        SELECT a FROM Affectation a
        WHERE a.collaborateur.id = :collaborateurId
        AND a.projet.id = :projetId
        AND a.dateDebut <= :dateFin
        AND a.dateFin >= :dateDebut
        ORDER BY a.dateDebut ASC
    """)
    List<Affectation> findAffectationsProjetSurPeriode(
            @Param("collaborateurId") Long collaborateurId,
            @Param("projetId") Long projetId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );
}
