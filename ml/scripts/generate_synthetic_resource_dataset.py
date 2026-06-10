#!/usr/bin/env python3
"""
Generate a synthetic project-month dataset for STAFF2STAFF ML experiments.

The generated data is not inserted into PostgreSQL and must only be used for
training/demo purposes in the PFE ML module.
"""

from __future__ import annotations

import csv
import random
from dataclasses import dataclass
from pathlib import Path


SEED = 20260609
ROW_COUNT = 5000
PROJECT_COUNT = 170
SIMULATED_MONTHS = 46

ROOT_DIR = Path(__file__).resolve().parents[1]
OUTPUT_FILE = ROOT_DIR / "datasets" / "synthetic_resource_forecasting_dataset.csv"

FIELDNAMES = [
    "projet_id",
    "mois",
    "annee",
    "duree_projet_jours",
    "nb_collaborateurs_actuels",
    "charge_moyenne",
    "charge_max",
    "nb_conflits",
    "nb_surcharges",
    "nb_sous_charges",
    "nb_anomalies_total",
    "nb_collaborateurs_concernes",
    "target_besoin_ressources_mois_suivant",
]


@dataclass(frozen=True)
class ProjectProfile:
    projet_id: int
    duree_projet_jours: int
    base_resources: int
    trend: str
    trend_rate: float
    budget_factor: float
    client_priority: float
    hr_availability: float
    volatility: float
    start_offset: int
    active_months: int


@dataclass
class MonthState:
    year: int
    month: int
    resources: int
    latent_demand: float
    budget_factor: float
    hr_availability: float
    phase_pressure: float
    atypical_event: float


def clamp(value: float, minimum: int, maximum: int) -> int:
    return max(minimum, min(maximum, int(round(value))))


def weighted_project_size() -> int:
    bucket = random.random()
    if bucket < 0.58:
        return random.randint(3, 18)
    if bucket < 0.9:
        return random.randint(19, 80)
    return random.randint(81, 320)


def weighted_trend() -> tuple[str, float]:
    bucket = random.random()
    if bucket < 0.34:
        return "GROWTH", random.uniform(0.006, 0.024)
    if bucket < 0.72:
        return "STABLE", random.uniform(-0.004, 0.005)
    return "DECLINE", random.uniform(-0.020, -0.006)


def build_project_profiles(count: int = PROJECT_COUNT) -> list[ProjectProfile]:
    profiles: list[ProjectProfile] = []
    for index in range(count):
        base_resources = weighted_project_size()
        trend, trend_rate = weighted_trend()
        active_months = random.randint(18, SIMULATED_MONTHS)
        profiles.append(
            ProjectProfile(
                projet_id=10000 + index,
                duree_projet_jours=random.randint(90, 920),
                base_resources=base_resources,
                trend=trend,
                trend_rate=trend_rate,
                budget_factor=random.uniform(0.72, 1.25),
                client_priority=random.uniform(0.72, 1.35),
                hr_availability=random.uniform(0.62, 1.22),
                volatility=random.uniform(0.05, 0.24),
                start_offset=random.randint(0, 8),
                active_months=active_months,
            )
        )
    return profiles


def add_months(start_year: int, start_month: int, offset: int) -> tuple[int, int]:
    month_index = start_month - 1 + offset
    return start_year + month_index // 12, month_index % 12 + 1


def seasonal_multiplier(month: int) -> float:
    if month in (3, 6, 9, 11):
        return random.uniform(1.03, 1.13)
    if month in (7, 8, 12):
        return random.uniform(0.88, 0.98)
    return random.uniform(0.96, 1.05)


def lifecycle_pressure(progress: float) -> float:
    if progress < 0.15:
        return random.uniform(0.55, 0.9)
    if progress < 0.38:
        return random.uniform(0.9, 1.25)
    if progress < 0.78:
        return random.uniform(1.0, 1.38)
    return random.uniform(0.55, 1.0)


def hidden_atypical_event(resources: int) -> float:
    event = random.random()
    if event < 0.055:
        return random.uniform(0.16, 0.42) * resources  # urgent client ramp-up
    if event < 0.120:
        return -random.uniform(0.14, 0.38) * resources  # budget freeze or scope cut
    if event < 0.180:
        return random.uniform(-0.28, 0.28) * resources  # staffing decision noise
    return 0.0


def simulate_project(profile: ProjectProfile) -> list[MonthState]:
    states: list[MonthState] = []
    resources = clamp(
        profile.base_resources * random.uniform(0.82, 1.15),
        1,
        360,
    )

    budget_factor = profile.budget_factor
    hr_availability = profile.hr_availability

    for local_month in range(profile.active_months + 1):
        global_month = profile.start_offset + local_month
        year, month = add_months(2024, 1, global_month)
        progress = local_month / max(1, profile.active_months)

        budget_factor = max(0.55, min(1.35, budget_factor + random.gauss(0, 0.025)))
        hr_availability = max(0.45, min(1.35, hr_availability + random.gauss(0, 0.035)))

        trend_multiplier = max(0.45, 1.0 + profile.trend_rate * local_month)
        phase_pressure = lifecycle_pressure(progress)
        latent_demand = (
            profile.base_resources
            * trend_multiplier
            * phase_pressure
            * seasonal_multiplier(month)
            * profile.client_priority
        )

        atypical_event = hidden_atypical_event(resources)
        desired_resources = latent_demand * budget_factor + atypical_event
        adjustment_capacity = max(1.0, resources * random.uniform(0.06, 0.22) * hr_availability)
        raw_delta = max(-adjustment_capacity, min(adjustment_capacity, desired_resources - resources))
        raw_delta += random.gauss(0, max(1.0, resources * profile.volatility * 0.45))

        resources = clamp(resources + raw_delta, 1, 380)
        states.append(
            MonthState(
                year=year,
                month=month,
                resources=resources,
                latent_demand=latent_demand,
                budget_factor=budget_factor,
                hr_availability=hr_availability,
                phase_pressure=phase_pressure,
                atypical_event=atypical_event,
            )
        )

    return states


def build_row(profile: ProjectProfile, state: MonthState, next_state: MonthState) -> dict[str, int | float]:
    demand_gap_ratio = (state.latent_demand - state.resources) / max(1, state.resources)
    hidden_delivery_pressure = (state.phase_pressure - 1.0) * random.uniform(6, 18)
    charge_moyenne = 88.0 + demand_gap_ratio * 34.0 + hidden_delivery_pressure + random.gauss(0, 12)
    charge_moyenne = max(42.0, min(158.0, charge_moyenne))
    charge_max = max(
        charge_moyenne,
        min(190.0, charge_moyenne + random.uniform(4, 42) + max(0.0, demand_gap_ratio) * 18),
    )

    overload_ratio = max(0.0, (charge_moyenne - 96.0) / 52.0)
    underload_ratio = max(0.0, (76.0 - charge_moyenne) / 42.0)
    conflict_ratio = max(0.0, (charge_max - 108.0) / 82.0)

    nb_surcharges = clamp(
        state.resources * overload_ratio * random.uniform(0.22, 0.95) + random.gauss(0, 2.4),
        0,
        state.resources,
    )
    nb_sous_charges = clamp(
        state.resources * underload_ratio * random.uniform(0.20, 0.82) + random.gauss(0, 1.8),
        0,
        state.resources,
    )
    nb_conflits = clamp(
        state.resources * conflict_ratio * random.uniform(0.02, 0.22) + random.gauss(0, 1.3),
        0,
        state.resources,
    )

    if random.random() < 0.045:
        nb_conflits = clamp(nb_conflits + random.randint(2, max(2, state.resources // 8)), 0, state.resources)
    if random.random() < 0.035:
        nb_surcharges = clamp(nb_surcharges + random.randint(1, max(1, state.resources // 10)), 0, state.resources)
    if random.random() < 0.04:
        nb_sous_charges = clamp(nb_sous_charges + random.randint(1, max(1, state.resources // 7)), 0, state.resources)

    total_anomalies = nb_conflits + nb_surcharges + nb_sous_charges
    concerned = clamp(total_anomalies * random.uniform(0.42, 0.92), 0, state.resources)

    hidden_next_month_revision = 0.0
    revision = random.random()
    if revision < 0.085:
        hidden_next_month_revision = random.uniform(0.14, 0.45) * next_state.resources
    elif revision < 0.185:
        hidden_next_month_revision = -random.uniform(0.12, 0.40) * next_state.resources
    elif revision < 0.285:
        hidden_next_month_revision = random.uniform(-0.24, 0.24) * next_state.resources

    target_noise = random.gauss(0, max(2.5, next_state.resources * 0.27))
    target = clamp(next_state.resources + hidden_next_month_revision + target_noise, 1, 380)

    row = {
        "projet_id": profile.projet_id,
        "mois": state.month,
        "annee": state.year,
        "duree_projet_jours": profile.duree_projet_jours,
        "nb_collaborateurs_actuels": state.resources,
        "charge_moyenne": round(charge_moyenne, 2),
        "charge_max": round(charge_max, 2),
        "nb_conflits": nb_conflits,
        "nb_surcharges": nb_surcharges,
        "nb_sous_charges": nb_sous_charges,
        "nb_anomalies_total": total_anomalies,
        "nb_collaborateurs_concernes": concerned,
        "target_besoin_ressources_mois_suivant": target,
    }
    validate_row(row)
    return row


def validate_row(row: dict[str, int | float]) -> None:
    if row["target_besoin_ressources_mois_suivant"] < 1:
        raise ValueError("target must be at least 1")
    if row["nb_anomalies_total"] != row["nb_conflits"] + row["nb_surcharges"] + row["nb_sous_charges"]:
        raise ValueError("anomaly total mismatch")
    if row["nb_collaborateurs_concernes"] > row["nb_collaborateurs_actuels"]:
        raise ValueError("concerned collaborators cannot exceed current collaborators")
    if row["charge_max"] < row["charge_moyenne"]:
        raise ValueError("max load cannot be below average load")


def generate_dataset(row_count: int = ROW_COUNT) -> list[dict[str, int | float]]:
    random.seed(SEED)
    profiles = build_project_profiles()
    rows: list[dict[str, int | float]] = []

    for profile in profiles:
        states = simulate_project(profile)
        for index in range(len(states) - 1):
            rows.append(build_row(profile, states[index], states[index + 1]))

    random.shuffle(rows)
    return rows[:row_count]


def main() -> None:
    rows = generate_dataset()
    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT_FILE.open("w", newline="", encoding="utf-8") as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=FIELDNAMES)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Generated {len(rows)} rows into {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
