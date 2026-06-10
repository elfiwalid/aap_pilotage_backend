#!/usr/bin/env python3
"""
Run one resource forecasting prediction from the saved STAFF2STAFF ML model.

Input is a JSON object passed as the first CLI argument. Output is a JSON object
printed to stdout. This script is intentionally isolated from Spring Boot.
"""

from __future__ import annotations

import json
import pickle
import sys
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[1]
MODEL_FILE = ROOT_DIR / "models" / "resource_forecasting_model.pkl"


def main() -> None:
    if len(sys.argv) < 2:
        raise SystemExit("Missing JSON input")

    payload = json.loads(sys.argv[1])
    with MODEL_FILE.open("rb") as model_file:
        saved_model = pickle.load(model_file)

    features = saved_model["features"]
    model = saved_model["model"]
    x = [[float(payload[feature]) for feature in features]]
    prediction = float(model.predict(x)[0])

    print(json.dumps({
        "modelName": saved_model["model_name"],
        "predictedResources": max(1, round(prediction)),
    }))


if __name__ == "__main__":
    main()
