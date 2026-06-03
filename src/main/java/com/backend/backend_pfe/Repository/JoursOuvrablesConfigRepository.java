package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.JoursOuvrablesConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JoursOuvrablesConfigRepository extends JpaRepository<JoursOuvrablesConfig, Long> {

    Optional<JoursOuvrablesConfig> findByAnneeAndMoisAndPays(int annee, int mois, String pays);
}
