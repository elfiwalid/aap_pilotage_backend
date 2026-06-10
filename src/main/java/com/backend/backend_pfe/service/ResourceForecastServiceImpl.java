package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.request.ResourceForecastRequestDTO;
import com.backend.backend_pfe.DTO.response.ResourceForecastResponseDTO;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResourceForecastServiceImpl implements ResourceForecastService {

    private static final Duration PYTHON_TIMEOUT = Duration.ofSeconds(8);
    private static final Pattern PREDICTED_RESOURCES_PATTERN =
            Pattern.compile("\"predictedResources\"\\s*:\\s*(\\d+)");

    @Override
    public ResourceForecastResponseDTO forecast(ResourceForecastRequestDTO request) {
        int currentResources = Math.max(0, request.getNbCollaborateursActuels());
        int predictedResources = runPythonModel(request);
        int difference = predictedResources - currentResources;

        return ResourceForecastResponseDTO.builder()
                .currentResources(currentResources)
                .predictedResources(predictedResources)
                .difference(difference)
                .riskLevel(resolveRiskLevel(currentResources, difference, request))
                .build();
    }

    private int runPythonModel(ResourceForecastRequestDTO request) {
        try {
            Path script = Path.of(System.getProperty("user.dir"), "ml", "scripts", "predict_resource_forecast.py");
            String payload = toPythonPayload(request);
            Process process = new ProcessBuilder("python", script.toString(), payload)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(PYTHON_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return fallbackForecast(request);
            }

            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().reduce("", (acc, line) -> acc + line);
            }

            if (process.exitValue() != 0 || output.isBlank()) {
                return fallbackForecast(request);
            }

            Matcher matcher = PREDICTED_RESOURCES_PATTERN.matcher(output);
            if (!matcher.find()) {
                return fallbackForecast(request);
            }
            return Math.max(1, Integer.parseInt(matcher.group(1)));
        } catch (Exception ignored) {
            return fallbackForecast(request);
        }
    }

    private String toPythonPayload(ResourceForecastRequestDTO request) {
        return "{"
                + "\"mois\":" + request.getMois()
                + ",\"annee\":" + request.getAnnee()
                + ",\"duree_projet_jours\":" + request.getDureeProjetJours()
                + ",\"nb_collaborateurs_actuels\":" + request.getNbCollaborateursActuels()
                + ",\"charge_moyenne\":" + request.getChargeMoyenne()
                + ",\"charge_max\":" + request.getChargeMax()
                + ",\"nb_conflits\":" + request.getNbConflits()
                + ",\"nb_surcharges\":" + request.getNbSurcharges()
                + ",\"nb_sous_charges\":" + request.getNbSousCharges()
                + ",\"nb_anomalies_total\":" + request.getNbAnomaliesTotal()
                + ",\"nb_collaborateurs_concernes\":" + request.getNbCollaborateursConcernes()
                + "}";
    }

    private int fallbackForecast(ResourceForecastRequestDTO request) {
        double predicted = Math.max(1, request.getNbCollaborateursActuels());
        predicted += Math.max(0, request.getChargeMoyenne() - 100.0) / 10.0;
        predicted += request.getNbSurcharges() * 0.18;
        predicted += request.getNbConflits() * 0.06;
        predicted -= request.getNbSousCharges() * 0.12;
        return Math.max(1, (int) Math.round(predicted));
    }

    private String resolveRiskLevel(int currentResources, int difference, ResourceForecastRequestDTO request) {
        int significantGap = Math.max(3, (int) Math.ceil(Math.max(1, currentResources) * 0.12));
        if (difference >= significantGap || request.getNbSurcharges() >= significantGap) {
            return "HIGH";
        }
        if (difference > 0 || request.getNbConflits() > 0 || request.getNbAnomaliesTotal() > significantGap) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
