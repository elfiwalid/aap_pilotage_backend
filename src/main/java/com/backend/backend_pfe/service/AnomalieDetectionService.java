package com.backend.backend_pfe.service;

import com.backend.backend_pfe.Entity.Projet;
import java.time.LocalDate;

public interface AnomalieDetectionService {
    void detecterAnomalies(Projet projet, LocalDate periodeDebut, LocalDate periodeFin);
}
