-- ═══════════════════════════════════════════════════════
-- Tables pour le module What-If : Simulation de remplacement
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS simulations_what_if (
    id BIGSERIAL PRIMARY KEY,
    type_simulation VARCHAR(50) NOT NULL,
    statut VARCHAR(30) NOT NULL,
    resultat VARCHAR(30),
    commentaire TEXT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    anomalie_id BIGINT REFERENCES anomalies_v2(id),
    resource_manager_id BIGINT REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS scenarios_what_if (
    id BIGSERIAL PRIMARY KEY,
    simulation_id BIGINT REFERENCES simulations_what_if(id),

    collaborateur_source_id BIGINT REFERENCES users(id),
    collaborateur_cible_id BIGINT REFERENCES users(id),
    projet_id BIGINT REFERENCES projets(id),

    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,

    taux_affectation DOUBLE PRECISION,

    jours_source_avant DOUBLE PRECISION,
    jours_source_apres DOUBLE PRECISION,
    jours_cible_avant DOUBLE PRECISION,
    jours_cible_apres DOUBLE PRECISION,

    taux_source_avant DOUBLE PRECISION,
    taux_source_apres DOUBLE PRECISION,
    taux_cible_avant DOUBLE PRECISION,
    taux_cible_apres DOUBLE PRECISION,

    conflit_corrige BOOLEAN,
    nouvelle_surcharge BOOLEAN,
    nouveau_conflit BOOLEAN,
    sous_charge_reduite BOOLEAN,

    commentaire TEXT
);
