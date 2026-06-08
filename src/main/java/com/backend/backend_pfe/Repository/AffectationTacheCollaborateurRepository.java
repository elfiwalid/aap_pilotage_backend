package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.Affectation;
import com.backend.backend_pfe.Entity.AffectationTacheCollaborateur;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AffectationTacheCollaborateurRepository
        extends JpaRepository<AffectationTacheCollaborateur, Long> {

    List<AffectationTacheCollaborateur> findByCollaborateurAndDateTacheBetweenOrderByDateTacheAscOrdreJourAsc(
            User collaborateur, LocalDate debut, LocalDate fin);

    List<AffectationTacheCollaborateur> findByProjetOrderByDateTacheAscOrdreJourAsc(Projet projet);

    List<AffectationTacheCollaborateur> findByProjetAndCollaborateurOrderByDateTacheAscOrdreJourAsc(
            Projet projet, User collaborateur);

    List<AffectationTacheCollaborateur> findByAffectationInAndDateTacheBetween(
            Collection<Affectation> affectations, LocalDate debut, LocalDate fin);

    void deleteByAffectationInAndDateTacheBetween(
            Collection<Affectation> affectations, LocalDate debut, LocalDate fin);
}
