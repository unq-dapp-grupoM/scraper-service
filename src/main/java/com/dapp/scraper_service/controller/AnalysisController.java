package com.dapp.scraper_service.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dapp.scraper_service.model.MatchStatistics;
import com.dapp.scraper_service.model.PerformanceMetrics;
import com.dapp.scraper_service.model.PredictiveAnalysis;
import com.dapp.scraper_service.service.DataIntegrationService;
import com.dapp.scraper_service.service.PerformanceCalculatorService;
import com.dapp.scraper_service.service.PredictionService;
import com.fasterxml.jackson.annotation.JsonFormat;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
public class AnalysisController {

    private final PredictionService predictionService;
    private final PerformanceCalculatorService performanceCalculator;
    private final DataIntegrationService dataIntegrationService;

    public AnalysisController(PredictionService predictionService,
            PerformanceCalculatorService performanceCalculator,
            DataIntegrationService dataIntegrationService) {
        this.predictionService = predictionService;
        this.performanceCalculator = performanceCalculator;
        this.dataIntegrationService = dataIntegrationService;
    }

    @GetMapping("/{player}/metrics")
    public ResponseEntity<?> getPerformanceMetrics(
            @PathVariable("player") String player,
            @RequestParam(name = "season", defaultValue = "2024") String season) {

        try {
            List<MatchStatistics> matches = dataIntegrationService.convertToMatchStatistics(player);

            if (matches.isEmpty()) {
                return ResponseEntity.status(404).body(
                        Map.of("error", "No data found",
                                "message", "No statistics found for player: " + player));
            }

            PerformanceMetrics metrics = performanceCalculator.calculateMetrics(matches);
            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Calculation error",
                            "message", e.getMessage()));
        }
    }

    @GetMapping("/{player}/prediction")
    public ResponseEntity<?> predictPerformance(
            @PathVariable("player") String player,
            @RequestParam(name = "opponent") String opponent,
            @RequestParam(name = "isHome") boolean isHome,
            @RequestParam(name = "position") String position) {

        try {
            PredictiveAnalysis prediction = predictionService.predictPerformance(
                    player, opponent, isHome, position);

            return ResponseEntity.ok(prediction);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Prediction error",
                            "message", e.getMessage()));
        }
    }

    @PostMapping("/{player}/convert-data")
    public ResponseEntity<Map<String, Object>> convertPlayerData(@PathVariable("player") String player) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<MatchStatistics> convertedStats = dataIntegrationService.convertToMatchStatistics(player);

            response.put("status", "SUCCESS");
            response.put("player", player);
            response.put("convertedMatches", convertedStats.size());
            response.put("message", "Data successfully converted from scraping format to analysis format");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("player", player);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/{player}/comparison")
    public ResponseEntity<Map<String, Object>> getComparativeAnalysis(
            @PathVariable("player") String player) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<MatchStatistics> allMatches = dataIntegrationService.convertToMatchStatistics(player);

            // Encontrar la temporada más reciente automáticamente
            Optional<String> latestSeason = allMatches.stream()
                    .map(MatchStatistics::getSeason)
                    .filter(Objects::nonNull)
                    .distinct()
                    .max(Comparator.naturalOrder());

            if (latestSeason.isPresent()) {
                String currentSeason = latestSeason.get();
                List<MatchStatistics> currentSeasonMatches = allMatches.stream()
                        .filter(m -> currentSeason.equals(m.getSeason()))
                        .toList();

                if (!currentSeasonMatches.isEmpty()) {
                    PerformanceMetrics metrics = performanceCalculator.calculateMetrics(currentSeasonMatches);
                    response.put("currentSeason", currentSeason);
                    response.put("metrics", metrics);
                    response.put("matchesAnalyzed", currentSeasonMatches.size());
                }
            }

            // También mostrar todas las temporadas disponibles para comparación
            Map<String, List<MatchStatistics>> matchesBySeason = allMatches.stream()
                    .filter(m -> m.getSeason() != null && !m.getSeason().isEmpty())
                    .collect(Collectors.groupingBy(MatchStatistics::getSeason));

            Map<String, PerformanceMetrics> allMetrics = new HashMap<>();
            for (Map.Entry<String, List<MatchStatistics>> entry : matchesBySeason.entrySet()) {
                PerformanceMetrics metrics = performanceCalculator.calculateMetrics(entry.getValue());
                allMetrics.put(entry.getKey(), metrics);
            }

            response.put("player", player);
            response.put("allSeasonsMetrics", allMetrics);
            response.put("availableSeasons", new ArrayList<>(matchesBySeason.keySet()));
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}