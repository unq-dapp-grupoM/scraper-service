package com.dapp.scraper_service.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            List<MatchStatistics> matches2024 = dataIntegrationService.convertToMatchStatistics(player);

            List<MatchStatistics> currentSeason = matches2024.stream()
                    .filter(m -> "2024".equals(m.getSeason()))
                    .toList();

            if (!currentSeason.isEmpty()) {
                PerformanceMetrics metrics2024 = performanceCalculator.calculateMetrics(currentSeason);
                response.put("2024", metrics2024);
            }

            response.put("player", player);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}