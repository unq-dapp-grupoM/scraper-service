package com.dapp.scraper_service.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dapp.scraper_service.model.MatchStatistics;
import com.dapp.scraper_service.model.PerformanceMetrics;
import com.dapp.scraper_service.repository.PerformanceMetricsRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PerformanceCalculatorService {

    private final PerformanceMetricsRepository performanceMetricsRepository;

    public PerformanceCalculatorService(PerformanceMetricsRepository performanceMetricsRepository) {
        this.performanceMetricsRepository = performanceMetricsRepository;
    }

    @Transactional
    public PerformanceMetrics calculateMetrics(List<MatchStatistics> matches) {
        PerformanceMetrics performanceMetrics = new PerformanceMetrics();

        List<MatchStatistics> playedMatches = matches.stream()
                .filter(m -> m.getMinutesPlayed() > 0)
                .collect(Collectors.toList());

        int totalMatches = playedMatches.size();
        if (totalMatches == 0) {
            if (!matches.isEmpty()) {
                performanceMetrics.setPlayerName(matches.get(0).getPlayerName());
            }
            return performanceMetrics;
        }

        performanceMetrics.setPlayerName(playedMatches.get(0).getPlayerName());

        // BASIC CALCULATIONS
        performanceMetrics.setGoalsPerMatch(calculateGoalsPerMatch(playedMatches));
        performanceMetrics.setAssistsPerMatch(calculateAssistsPerMatch(playedMatches));
        performanceMetrics.setGoalInvolvement(calculateGoalInvolvement(playedMatches));
        performanceMetrics.setShotsPerMatch(calculateShotsPerMatch(playedMatches));
        performanceMetrics.setPassAccuracy(calculatePassAccuracy(playedMatches));
        performanceMetrics.setAverageRating(calculateAverageRating(playedMatches));

        // NEW: Calculate rating deviation for consistency
        performanceMetrics.setRatingDeviation(calculateRatingDeviation(playedMatches, performanceMetrics.getAverageRating()));

        performanceMetrics.setShotAccuracy(calculateShotAccuracy(playedMatches));
        performanceMetrics.setKeyPassesPerMatch(calculateKeyPassesPerMatch(playedMatches));
        performanceMetrics.setAerialDuelsWon(calculateAerialDuelsWon(playedMatches));
        performanceMetrics.setRecoveriesPerMatch(calculateRecoveriesPerMatch(playedMatches));
        performanceMetrics.setMinutesPerMatch(calculateMinutesPerMatch(playedMatches));

        // ADVANCED PREDICTION METRICS
        performanceMetrics.setGoalProbability(calculateGoalProbability(playedMatches));
        performanceMetrics.setAssistProbability(calculateAssistProbability(playedMatches));
        performanceMetrics.setOffensiveImpact(calculateOffensiveImpact(playedMatches));
        performanceMetrics.setPerformanceTrend(calculatePerformanceTrend(playedMatches));

        return performanceMetricsRepository.save(performanceMetrics);
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
        // IMPROVED: Estimation based on actual goals
        double goalsPerGame = calculateGoalsPerMatch(matches);
        double shotsPerGame = calculateShotsPerMatch(matches);

        if (shotsPerGame == 0)
            return 0.3; // Default if there are no shots

        // If he scores many goals with few shots -> good accuracy
        double efficiency = goalsPerGame / shotsPerGame;
        return Math.min(0.8, Math.max(0.2, efficiency * 3.0)); // Realistic range 20%-80%
    }

    private Double calculateKeyPassesPerMatch(List<MatchStatistics> matches) {
        // IMPROVED: Based on actual assists
        double assistsPerGame = calculateAssistsPerMatch(matches);

        // Realistic ratio: for each assist there are ~2-3 key passes
        return Math.max(0.5, assistsPerGame * 2.5);
    }

    private Double calculateAerialDuelsWon(List<MatchStatistics> matches) {
        int totalAerials = matches.stream().mapToInt(MatchStatistics::getAerialDuels).sum();
        return (double) totalAerials / matches.size();
    }

    private Double calculateRecoveriesPerMatch(List<MatchStatistics> matches) {
        // IMPROVED: Based on position and defensive performance
        if (matches.isEmpty())
            return 2.0;

        String position = matches.get(0).getPosition();
        double avgRating = calculateAverageRating(matches);

        if (position != null && (position.toLowerCase().contains("def") || position.toLowerCase().contains("df"))) {
            return 4.0 + (avgRating - 6.0); // Defenders: more recoveries
        } else if (position != null
                && (position.toLowerCase().contains("mid") || position.toLowerCase().contains("mf"))) {
            return 2.5 + (avgRating - 6.0) * 0.5; // Midfielders: medium
        } else {
            return 1.0 + (avgRating - 6.0) * 0.3; // Forwards: less
        }
    }

    private Double calculateMinutesPerMatch(List<MatchStatistics> matches) {
        int totalMinutes = matches.stream().mapToInt(MatchStatistics::getMinutesPlayed).sum();
        return (double) totalMinutes / matches.size();
    }
}
