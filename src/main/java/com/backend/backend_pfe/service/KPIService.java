package com.backend.backend_pfe.service;

import java.util.Map;

public interface KPIService {
    Double calculerTauxOccupationGlobal();
    Double calculerTNF();
    Map<String, Double> getOccupationParCollaborateur();
    Map<String, Double> getEvolutionOccupationMensuelle();
}
