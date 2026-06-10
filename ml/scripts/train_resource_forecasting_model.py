#!/usr/bin/env python3
"""
Train resource forecasting models for STAFF2STAFF ML experiments.

This script is isolated from the Spring Boot application. It reads the synthetic
CSV dataset, compares Linear Regression and Random Forest Regressor, then stores
the best model and its metrics under ml/models/.
"""

from __future__ import annotations

import csv
import json
import math
import pickle
from pathlib import Path

try:
    from sklearn.ensemble import RandomForestRegressor
    from sklearn.linear_model import LinearRegression
    from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
    from sklearn.model_selection import train_test_split
    from sklearn.pipeline import Pipeline
    from sklearn.preprocessing import StandardScaler
except ModuleNotFoundError as exc:
    raise SystemExit(
        "scikit-learn is required for this training phase. "
        "Install it with: python -m pip install scikit-learn"
    ) from exc


RANDOM_STATE = 20260609
TEST_SIZE = 0.2

ROOT_DIR = Path(__file__).resolve().parents[1]
DATASET_FILE = ROOT_DIR / "datasets" / "synthetic_resource_forecasting_dataset.csv"
MODELS_DIR = ROOT_DIR / "models"
MODEL_FILE = MODELS_DIR / "resource_forecasting_model.pkl"
METRICS_FILE = MODELS_DIR / "resource_forecasting_metrics.json"

FEATURES = [
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
]
TARGET = "target_besoin_ressources_mois_suivant"


def load_dataset() -> tuple[list[list[float]], list[float]]:
    if not DATASET_FILE.exists():
        raise FileNotFoundError(f"Dataset not found: {DATASET_FILE}")

    x: list[list[float]] = []
    y: list[float] = []
    with DATASET_FILE.open(newline="", encoding="utf-8") as csv_file:
        reader = csv.DictReader(csv_file)
        missing_columns = [column for column in [*FEATURES, TARGET] if column not in reader.fieldnames]
        if missing_columns:
            raise ValueError(f"Missing columns in dataset: {', '.join(missing_columns)}")

        for row in reader:
            x.append([float(row[feature]) for feature in FEATURES])
            y.append(float(row[TARGET]))

    if len(x) < 100:
        raise ValueError(f"Dataset is too small for training: {len(x)} rows")

    return x, y


def evaluate_model(model, x_test: list[list[float]], y_test: list[float]) -> dict[str, float]:
    predictions = model.predict(x_test)
    mae = mean_absolute_error(y_test, predictions)
    rmse = math.sqrt(mean_squared_error(y_test, predictions))
    r2 = r2_score(y_test, predictions)
    return {
        "mae": round(float(mae), 4),
        "rmse": round(float(rmse), 4),
        "r2": round(float(r2), 4),
    }


def train() -> dict:
    x, y = load_dataset()
    x_train, x_test, y_train, y_test = train_test_split(
        x,
        y,
        test_size=TEST_SIZE,
        random_state=RANDOM_STATE,
    )

    candidates = {
        "Linear Regression": Pipeline(
            steps=[
                ("scaler", StandardScaler()),
                ("model", LinearRegression()),
            ]
        ),
        "Random Forest Regressor": RandomForestRegressor(
            n_estimators=250,
            max_depth=14,
            min_samples_leaf=2,
            random_state=RANDOM_STATE,
            n_jobs=1,
        ),
    }

    metrics: dict[str, dict[str, float]] = {}
    trained_models = {}
    for name, model in candidates.items():
        model.fit(x_train, y_train)
        metrics[name] = evaluate_model(model, x_test, y_test)
        trained_models[name] = model

    best_model_name = min(metrics, key=lambda model_name: metrics[model_name]["rmse"])
    best_model = trained_models[best_model_name]

    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    with MODEL_FILE.open("wb") as model_file:
        pickle.dump(
            {
                "model_name": best_model_name,
                "features": FEATURES,
                "target": TARGET,
                "model": best_model,
            },
            model_file,
        )

    result = {
        "dataset": str(DATASET_FILE),
        "rows": len(x),
        "features": FEATURES,
        "target": TARGET,
        "test_size": TEST_SIZE,
        "random_state": RANDOM_STATE,
        "models": metrics,
        "best_model": best_model_name,
        "selection_metric": "rmse",
        "model_file": str(MODEL_FILE),
    }
    with METRICS_FILE.open("w", encoding="utf-8") as metrics_file:
        json.dump(result, metrics_file, indent=2)

    return result


def main() -> None:
    result = train()
    print(f"Trained {len(result['models'])} models on {result['rows']} rows")
    print(f"Best model: {result['best_model']}")
    print(f"Model saved to: {MODEL_FILE}")
    print(f"Metrics saved to: {METRICS_FILE}")


if __name__ == "__main__":
    main()
