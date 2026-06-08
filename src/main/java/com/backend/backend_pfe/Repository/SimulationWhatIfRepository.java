package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.SimulationWhatIf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SimulationWhatIfRepository extends JpaRepository<SimulationWhatIf, Long> {

    @Modifying
    @Query("""
        UPDATE SimulationWhatIf s SET s.anomalie = null
        WHERE s.anomalie.id IN (
            SELECT a.id FROM AnomalieV2 a WHERE a.annee = :annee AND a.mois = :mois
        )
    """)
    void nullifyAnomalieByPeriode(@Param("annee") int annee, @Param("mois") int mois);
}
