package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.AnomalieV2;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.enums.StatutAnomalieV2;
import com.backend.backend_pfe.enums.TypeAnomalieV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface AnomalieV2Repository extends JpaRepository<AnomalieV2, Long> {

    List<AnomalieV2> findByAnneeAndMoisOrderByDateDetectionDesc(int annee, int mois);

    List<AnomalieV2> findByAnneeAndMoisAndTypeAnomalie(int annee, int mois, TypeAnomalieV2 type);

    List<AnomalieV2> findByCollaborateur(User collaborateur);

    List<AnomalieV2> findByAnneeAndMoisAndStatut(int annee, int mois, StatutAnomalieV2 statut);

    Optional<AnomalieV2> findByCleDeduplication(String cleDeduplication);

    @Modifying
    void deleteByAnneeAndMois(int annee, int mois);
}
