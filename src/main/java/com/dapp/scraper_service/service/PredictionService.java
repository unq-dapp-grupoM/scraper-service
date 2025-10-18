package com.dapp.scraper_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.dapp.scraper_service.model.MatchStatistics;
import com.dapp.scraper_service.model.PerformanceMetrics;
import com.dapp.scraper_service.model.PredictiveAnalysis;
import com.dapp.scraper_service.repository.MatchStatisticsRepository;
import com.dapp.scraper_service.repository.PredictiveAnalysisRepository;

@Service
public class PredictionService {

    private final PerformanceCalculatorService performanceCalculator;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final PredictiveAnalysisRepository predictiveAnalysisRepository;

    public PredictionService(PerformanceCalculatorService performanceCalculator,
            MatchStatisticsRepository matchStatisticsRepository,
            PredictiveAnalysisRepository predictiveAnalysisRepository) {
        this.performanceCalculator = performanceCalculator;
        this.matchStatisticsRepository = matchStatisticsRepository;
        this.predictiveAnalysisRepository = predictiveAnalysisRepository;
    }

    public PredictiveAnalysis predictPerformance(String playerName,
            String opponent,
            boolean isHome,
            String position) {

        List<MatchStatistics> historicalData = getPlayerHistoricalData(playerName);
        PerformanceMetrics metrics = performanceCalculator.calculateMetrics(historicalData);

        PredictiveAnalysis prediction = new PredictiveAnalysis(playerName);

        // CALCULATE BASE PROBABILITIES
        prediction.setGoalProbability(calculateGoalProbability(metrics, opponent, isHome));
        prediction.setAssistProbability(calculateAssistProbability(metrics, opponent, isHome));
        prediction.setHighRatingProbability(calculateHighRatingProbability(metrics));
        prediction.setFullMatchProbability(calculateFullMatchProbability(historicalData));

        // CONTEXTUAL FACTORS
        prediction.setHomeAdvantageFactor(calculateHomeAdvantageFactor(historicalData, isHome));
        prediction.setOpponentFactor(calculateOpponentFactor(historicalData, opponent));
        prediction.setPositionFactor(calculatePositionFactor(historicalData, position));
        prediction.setTrendFactor(metrics.getPerformanceTrend());

        // FINAL PREDICTIVE SCORE
        prediction.setPredictiveScore(calculatePredictiveScore(prediction));
        prediction.setPerformancePrediction(determinePerformanceLevel(prediction.getPredictiveScore()));

        return predictiveAnalysisRepository.save(prediction);
    }

    private List<MatchStatistics> getPlayerHistoricalData(String playerName) {
        List<MatchStatistics> allMatches = matchStatisticsRepository.findByPlayerName(playerName);

        return allMatches.stream()
                .filter(match -> "2024".equals(match.getSeason()))
                .collect(Collectors.toList());
    }

    private Double calculateGoalProbability(PerformanceMetrics metrics, String opponent, boolean isHome) {
        double baseProbability = metrics.getGoalProbability();
        double opponentFactor = calculateHistoricalOpponentFactor(opponent);
        double homeAdvantage = isHome ? 1.1 : 0.9;

        return Math.min(0.95, baseProbability * opponentFactor * homeAdvantage);
    }

    private Double calculateAssistProbability(PerformanceMetrics metrics, String opponent, boolean isHome) {
        double baseProbability = metrics.getAssistProbability();
        double opponentFactor = calculateHistoricalOpponentFactor(opponent);
        double homeAdvantage = isHome ? 1.05 : 0.95; // Smaller effect for assists

        return Math.min(0.95, baseProbability * opponentFactor * homeAdvantage);
    }

    private Double calculateHighRatingProbability(PerformanceMetrics metrics) {
        // Probability of getting rating > 7.5 based on historical performance
        double averageRating = metrics.getAverageRating();
        double ratingDeviation = metrics.getRatingDeviation() != null ? metrics.getRatingDeviation() : 1.0; // Default
                                                                                                            // deviation

        // Using normal distribution approximation
        double zScore = (7.5 - averageRating) / ratingDeviation;
        double probability = 1 - cumulativeDistributionFunction(zScore);

        return Math.max(0.0, Math.min(1.0, probability));
    }

    private Double calculateFullMatchProbability(List<MatchStatistics> historicalData) {
        long fullMatches = historicalData.stream()
                .filter(m -> m.getMinutesPlayed() >= 85) // Consider 85+ mins as full match
                .count();

        return historicalData.isEmpty() ? 0.0 : (double) fullMatches / historicalData.size();
    }

    private Double calculateOpponentFactor(List<MatchStatistics> historicalData, String opponent) {
        // Filter matches against this specific opponent
        List<MatchStatistics> matchesVsOpponent = historicalData.stream()
                .filter(m -> opponent.equalsIgnoreCase(m.getOpponent()))
                .collect(Collectors.toList());

        if (matchesVsOpponent.isEmpty()) {
            return 1.0; // Neutral factor if no historical data
        }

        // Calculate average performance against this opponent
        double avgRatingVsOpponent = matchesVsOpponent.stream()
                .mapToDouble(MatchStatistics::getRating)
                .average()
                .orElse(6.0);

        // Calculate overall average performance
        double overallAvgRating = historicalData.stream()
                .mapToDouble(MatchStatistics::getRating)
                .average()
                .orElse(6.0);

        // Return performance ratio (>1 = better vs this opponent)
        return avgRatingVsOpponent / overallAvgRating;
    }

    private Double calculatePositionFactor(List<MatchStatistics> historicalData, String position) {
        // Filter matches in this specific position
        List<MatchStatistics> matchesInPosition = historicalData.stream()
                .filter(m -> position.equalsIgnoreCase(m.getPosition()))
                .collect(Collectors.toList());

        if (matchesInPosition.isEmpty()) {
            return 1.0; // Neutral factor if no data for this position
        }

        // Calculate average performance in this position
        double avgRatingInPosition = matchesInPosition.stream()
                .mapToDouble(MatchStatistics::getRating)
                .average()
                .orElse(6.0);

        // Calculate overall average performance
        double overallAvgRating = historicalData.stream()
                .mapToDouble(MatchStatistics::getRating)
                .average()
                .orElse(6.0);

        // Return performance ratio (>1 = better in this position)
        return avgRatingInPosition / overallAvgRating;
    }

    private Double calculateHomeAdvantageFactor(List<MatchStatistics> historicalData, boolean isHome) {
        List<MatchStatistics> homeMatches = historicalData.stream()
                .filter(m -> "H".equals(m.getResult()))
                .collect(Collectors.toList());

        List<MatchStatistics> awayMatches = historicalData.stream()
                .filter(m -> "A".equals(m.getResult()))
                .collect(Collectors.toList());

        if (homeMatches.isEmpty() || awayMatches.isEmpty()) {
            return isHome ? 1.1 : 0.9; // Default factors
        }

        double homePerformance = homeMatches.stream()
                .mapToDouble(MatchStatistics::getRating)
                .average().orElse(6.0);

        double awayPerformance = awayMatches.stream()
                .mapToDouble(MatchStatistics::getRating)
                .average().orElse(6.0);

        double performanceRatio = homePerformance / awayPerformance;

        // Normalize to reasonable range
        return isHome ? Math.min(1.3, Math.max(0.7, performanceRatio))
                : Math.min(1.3, Math.max(0.7, 1.0 / performanceRatio));
    }

    // Helper method for normal distribution CDF
    private Double cumulativeDistributionFunction(double z) {
        // Simplified CDF approximation
        return 0.5 * (1 + erf(z / Math.sqrt(2)));
    }

    private Double erf(double x) {
        // Error function approximation
        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        double p = 0.3275911;

        int sign = (x < 0) ? -1 : 1;
        x = Math.abs(x);

        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);

        return sign * y;
    }

    private Double calculateHistoricalOpponentFactor(String opponent) {
        // This could be enhanced with opponent strength data
        // For now, return neutral factor
        return 1.0;
    }

    private Double calculatePredictiveScore(PredictiveAnalysis prediction) {
        double score = 0.0;

        score += prediction.getGoalProbability() * 30;
        score += prediction.getAssistProbability() * 25;
        score += prediction.getHighRatingProbability() * 20;
        score += prediction.getFullMatchProbability() * 10;
        score += (prediction.getTrendFactor() + 1) * 5;
        score += (prediction.getHomeAdvantageFactor() - 1) * 5;
        score += (prediction.getOpponentFactor() - 1) * 3;
        score += (prediction.getPositionFactor() - 1) * 2;

        return Math.min(100, Math.max(0, score));
    }

    private String determinePerformanceLevel(Double score) {
        if (score >= 75)
            return "HIGH";
        if (score >= 50)
            return "MEDIUM";
        return "LOW";
    }
}