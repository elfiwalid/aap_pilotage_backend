package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.ScenarioWhatIf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScenarioWhatIfRepository extends JpaRepository<ScenarioWhatIf, Long> {

    Optional<ScenarioWhatIf> findBySimulationId(Long simulationId);
}
