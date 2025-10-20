package com.dapp.scraper_service.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dapp.scraper_service.model.MatchStatistics;
import com.dapp.scraper_service.model.PerformanceMetrics;
import com.dapp.scraper_service.repository.PerformanceMetricsRepository;

@Service
public class PerformanceCalculatorService {

    private final PerformanceMetricsRepository metricsRepository;

    public PerformanceCalculatorService(PerformanceMetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    public PerformanceMetrics calculateMetrics(List<MatchStatistics> matches) {
        PerformanceMetrics metrics = new PerformanceMetrics();

        List<MatchStatistics> playedMatches = matches.stream()
                .filter(m -> m.getMinutesPlayed() > 0)
                .collect(Collectors.toList());

        int totalMatches = playedMatches.size();
        if (totalMatches == 0) {
            if (!matches.isEmpty()) {
                metrics.setPlayerName(matches.get(0).getPlayerName());
            }
            return metrics;
        }

        metrics.setPlayerName(playedMatches.get(0).getPlayerName());

        // BASIC CALCULATIONS
        metrics.setGoalsPerMatch(calculateGoalsPerMatch(playedMatches));
        metrics.setAssistsPerMatch(calculateAssistsPerMatch(playedMatches));
        metrics.setGoalInvolvement(calculateGoalInvolvement(playedMatches));
        metrics.setShotsPerMatch(calculateShotsPerMatch(playedMatches));
        metrics.setPassAccuracy(calculatePassAccuracy(playedMatches));
        metrics.setAverageRating(calculateAverageRating(playedMatches));

        // NEW: Calculate rating deviation for consistency
        metrics.setRatingDeviation(calculateRatingDeviation(playedMatches, metrics.getAverageRating()));

        metrics.setShotAccuracy(calculateShotAccuracy(playedMatches));
        metrics.setKeyPassesPerMatch(calculateKeyPassesPerMatch(playedMatches));
        metrics.setAerialDuelsWon(calculateAerialDuelsWon(playedMatches));
        metrics.setRecoveriesPerMatch(calculateRecoveriesPerMatch(playedMatches));
        metrics.setMinutesPerMatch(calculateMinutesPerMatch(playedMatches));

        // ADVANCED PREDICTION METRICS
        metrics.setGoalProbability(calculateGoalProbability(playedMatches));
        metrics.setAssistProbability(calculateAssistProbability(playedMatches));
        metrics.setOffensiveImpact(calculateOffensiveImpact(playedMatches));
        metrics.setPerformanceTrend(calculatePerformanceTrend(playedMatches));

        return metricsRepository.save(metrics);
    }

    private Double calculateRatingDeviation(List<MatchStatistics> matches, Double averageRating) {
        if (matches.size() < 2)
            return 1.0; // Default deviation for small sample

        double variance = matches.stream()
                .mapToDouble(m -> Math.pow(m.getRating() - averageRating, 2))
                .sum() / (matches.size() - 1);

        return Math.sqrt(variance);
    }

    private Double calculateGoalsPerMatch(List<MatchStatistics> matches) {
        int totalGoals = matches.stream().mapToInt(MatchStatistics::getGoals).sum();
        return (double) totalGoals / matches.size();
    }

    private Double calculateAssistsPerMatch(List<MatchStatistics> matches) {
        int totalAssists = matches.stream().mapToInt(MatchStatistics::getAssists).sum();
        return (double) totalAssists / matches.size();
    }

    private Double calculateGoalInvolvement(List<MatchStatistics> matches) {
        int totalGoals = matches.stream().mapToInt(MatchStatistics::getGoals).sum();
        int totalAssists = matches.stream().mapToInt(MatchStatistics::getAssists).sum();
        return (double) (totalGoals + totalAssists) / matches.size();
    }

    private Double calculateShotsPerMatch(List<MatchStatistics> matches) {
        int totalShots = matches.stream().mapToInt(MatchStatistics::getShots).sum();
        return (double) totalShots / matches.size();
    }

    private Double calculatePassAccuracy(List<MatchStatistics> matches) {
        return matches.stream()
                .mapToDouble(MatchStatistics::getPassAccuracy)
                .average()
                .orElse(0.0);
    }

    private Double calculateAverageRating(List<MatchStatistics> matches) {
        return matches.stream()
                .mapToDouble(MatchStatistics::getRating)
                .average()
                .orElse(0.0);
    }

    private Double calculateGoalProbability(List<MatchStatistics> matches) {
        long matchesWithGoals = matches.stream()
                .filter(m -> m.getGoals() > 0)
                .count();
        return (double) matchesWithGoals / matches.size();
    }

    private Double calculateAssistProbability(List<MatchStatistics> matches) {
        long matchesWithAssists = matches.stream()
                .filter(m -> m.getAssists() > 0)
                .count();
        return (double) matchesWithAssists / matches.size();
    }

    private Double calculateOffensiveImpact(List<MatchStatistics> matches) {
        // Weighted formula: Goals (40%), Assists (30%), Rating (20%), Shots (10%)
        double avgGoals = calculateGoalsPerMatch(matches);
        double avgAssists = calculateAssistsPerMatch(matches);
        double avgRating = calculateAverageRating(matches);
        double avgShots = calculateShotsPerMatch(matches);

        return (avgGoals * 0.4) + (avgAssists * 0.3) +
                (avgRating * 0.2 / 10) + (avgShots * 0.1 / 10);
    }

    private Double calculatePerformanceTrend(List<MatchStatistics> matches) {
        // Sort by date (most recent first)
        List<MatchStatistics> sortedMatches = matches.stream()
                .sorted(Comparator.comparing(MatchStatistics::getMatchDate).reversed())
                .collect(Collectors.toList());

        // Compare recent 5 matches vs first 5
        int n = Math.min(5, sortedMatches.size() / 2);
        if (n < 2)
            return 0.0;

        double recentRating = sortedMatches.subList(0, n).stream()
                .mapToDouble(MatchStatistics::getRating)
                .average().orElse(0.0);

        double olderRating = sortedMatches.subList(sortedMatches.size() - n, sortedMatches.size()).stream()
                .mapToDouble(MatchStatistics::getRating)
                .average().orElse(0.0);

        return recentRating - olderRating;
    }

    private Double calculateShotAccuracy(List<MatchStatistics> matches) {
        // Placeholder - necesitarías datos de shots on target vs total shots
        return 0.5; // 50% por defecto
    }

    private Double calculateKeyPassesPerMatch(List<MatchStatistics> matches) {
        // Placeholder - necesitarías datos de pases clave
        return 1.5; // valor por defecto
    }

    private Double calculateAerialDuelsWon(List<MatchStatistics> matches) {
        int totalAerials = matches.stream().mapToInt(MatchStatistics::getAerialDuels).sum();
        return (double) totalAerials / matches.size();
    }

    private Double calculateRecoveriesPerMatch(List<MatchStatistics> matches) {
        // Placeholder - necesitarías datos de recuperaciones
        return 2.0; // valor por defecto
    }

    private Double calculateMinutesPerMatch(List<MatchStatistics> matches) {
        int totalMinutes = matches.stream().mapToInt(MatchStatistics::getMinutesPlayed).sum();
        return (double) totalMinutes / matches.size();
    }
}
