package com.dapp.scraper_service.service;

import com.dapp.scraper_service.model.dto.TeamComparisonDTO;
import com.dapp.scraper_service.model.dto.TeamDTO;
import com.dapp.scraper_service.model.dto.TeamPlayerDTO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TeamComparisonService {

    private final TeamService teamService;

    public TeamComparisonService(TeamService teamService) {
        this.teamService = teamService;
    }

    public TeamComparisonDTO compareTeams(String team1Name, String team2Name) {
        // Obtener datos de ambos equipos
        List<TeamDTO> team1List = teamService.getTeamInfoByName(team1Name);
        List<TeamDTO> team2List = teamService.getTeamInfoByName(team2Name);

        if (team1List.isEmpty() || team2List.isEmpty()) {
            throw new IllegalArgumentException("One or both teams not found");
        }

        TeamDTO team1 = team1List.getFirst();
        TeamDTO team2 = team2List.getFirst();

        return buildComparison(team1, team2, team1Name, team2Name);
    }

    private TeamComparisonDTO buildComparison(TeamDTO team1, TeamDTO team2, String team1Name, String team2Name) {
        TeamComparisonDTO comparison = new TeamComparisonDTO();
        comparison.setTeamName1(team1Name);
        comparison.setTeamName2(team2Name);

        TeamComparisonDTO.ComparisonMetrics metrics = new TeamComparisonDTO.ComparisonMetrics();

        // Calculate comparison metrics
        metrics.setTeam1OverallRating(round(calculateOverallRating(team1)));
        metrics.setTeam2OverallRating(round(calculateOverallRating(team2)));
        metrics.setPositionalAdvantages(calculatePositionalAdvantages(team1, team2));
        metrics.setKeyMetricsComparison(calculateKeyMetrics(team1, team2));

        // Improved prediction
        TeamComparisonDTO.Prediction prediction = predictMatchOutcome(metrics, team1Name, team2Name);
        metrics.setPrediction(prediction);

        comparison.setComparison(metrics);
        comparison.setSuggestedWinner(generateWinnerMessage(prediction, metrics, team1Name, team2Name));
        comparison.setConfidenceLevel(round(calculateConfidence(metrics)));

        return comparison;
    }

    private double calculateOverallRating(TeamDTO team) {
        if (team.getSquad() == null || team.getSquad().isEmpty()) {
            return 6.0;
        }

        return team.getSquad().stream()
                .mapToDouble(player -> parseDouble(player.getRating(), 6.0))
                .average()
                .orElse(6.0);
    }

    private Map<String, Double> calculatePositionalAdvantages(TeamDTO team1, TeamDTO team2) {
        Map<String, Double> advantages = new HashMap<>();

        Map<String, Double> team1Positions = groupPlayersByPosition(team1);
        Map<String, Double> team2Positions = groupPlayersByPosition(team2);

        Set<String> allPositions = new HashSet<>();
        allPositions.addAll(team1Positions.keySet());
        allPositions.addAll(team2Positions.keySet());

        for (String position : allPositions) {
            double team1Avg = team1Positions.getOrDefault(position, 6.0);
            double team2Avg = team2Positions.getOrDefault(position, 6.0);
            double advantage = round(team1Avg - team2Avg);
            // Only include significant advantages (> 0.1)
            if (Math.abs(advantage) >= 0.1) {
                advantages.put(position, advantage);
            }
        }

        return advantages;
    }

    private Map<String, Double> groupPlayersByPosition(TeamDTO team) {
        if (team.getSquad() == null) {
            return new HashMap<>();
        }

        return team.getSquad().stream()
                .collect(Collectors.groupingBy(
                        player -> normalizePosition(player.getPosition()),
                        Collectors.averagingDouble(p -> parseDouble(p.getRating(), 6.0))
                ));
    }

    private String normalizePosition(String position) {
        if (position == null) return "Unknown";
        String pos = position.toLowerCase();

        if (pos.contains("def") || pos.contains("df") || pos.contains("defensa")) return "Defense";
        if (pos.contains("mid") || pos.contains("mf") || pos.contains("cm") || pos.contains("mediocamp")) return "Midfield";
        if (pos.contains("for") || pos.contains("fw") || pos.contains("st") || pos.contains("delanter")) return "Forward";
        if (pos.contains("gk") || pos.contains("portero") || pos.contains("arquero")) return "Goalkeeper";

        return "Midfield";
    }

    private Map<String, Double> calculateKeyMetrics(TeamDTO team1, TeamDTO team2) {
        Map<String, Double> metrics = new HashMap<>();

        // Only include metrics with significant differences
        double goalsDiff = round(calculateTeamMetric(team1, TeamPlayerDTO::getGoals) -
                calculateTeamMetric(team2, TeamPlayerDTO::getGoals));
        double assistsDiff = round(calculateTeamMetric(team1, TeamPlayerDTO::getAssists) -
                calculateTeamMetric(team2, TeamPlayerDTO::getAssists));
        double defenseDiff = round(calculateDefensiveStrength(team1) -
                calculateDefensiveStrength(team2));
        double passDiff = round(calculateTeamMetric(team1, TeamPlayerDTO::getPassSuccess) -
                calculateTeamMetric(team2, TeamPlayerDTO::getPassSuccess));
        double efficiencyDiff = round(calculateOffensiveEfficiency(team1) -
                calculateOffensiveEfficiency(team2));
        double disciplineDiff = round(calculateDiscipline(team2) - calculateDiscipline(team1));

        if (Math.abs(goalsDiff) >= 0.1) metrics.put("goalsPerGame", goalsDiff);
        if (Math.abs(assistsDiff) >= 0.1) metrics.put("assistsPerGame", assistsDiff);
        if (Math.abs(defenseDiff) >= 0.1) metrics.put("defensiveStrength", defenseDiff);
        if (Math.abs(passDiff) >= 1.0) metrics.put("passAccuracy", passDiff);
        if (Math.abs(efficiencyDiff) >= 5.0) metrics.put("offensiveEfficiency", efficiencyDiff);
        if (Math.abs(disciplineDiff) >= 0.1) metrics.put("discipline", disciplineDiff);

        return metrics;
    }

    private double calculateTeamMetric(TeamDTO team, Function<TeamPlayerDTO, String> mapper) {
        if (team.getSquad() == null || team.getSquad().isEmpty()) {
            return 0.0;
        }

        return team.getSquad().stream()
                .mapToDouble(player -> {
                    String value = mapper.apply(player);
                    return parseDouble(value, 0.0);
                })
                .average()
                .orElse(0.0);
    }

    private double calculateDefensiveStrength(TeamDTO team) {
        if (team.getSquad() == null) {
            return 6.0;
        }

        return team.getSquad().stream()
                .filter(player -> {
                    String position = normalizePosition(player.getPosition());
                    return position.equals("Defense") || position.equals("Goalkeeper");
                })
                .mapToDouble(player -> parseDouble(player.getRating(), 6.0))
                .average()
                .orElse(6.0);
    }

    private double calculateOffensiveEfficiency(TeamDTO team) {
        if (team.getSquad() == null || team.getSquad().isEmpty()) {
            return 0.0;
        }

        double totalGoals = calculateTeamMetric(team, TeamPlayerDTO::getGoals);
        double totalShots = calculateTeamMetric(team, TeamPlayerDTO::getShotsPerGame);

        if (totalShots == 0) return 0.0;
        return (totalGoals / totalShots) * 100;
    }

    private double calculateDiscipline(TeamDTO team) {
        if (team.getSquad() == null || team.getSquad().isEmpty()) {
            return 0.0;
        }

        // Lower values are better (fewer cards)
        double yellowCards = calculateTeamMetric(team, TeamPlayerDTO::getYellowCards);
        double redCards = calculateTeamMetric(team, TeamPlayerDTO::getRedCards);

        return yellowCards + (redCards * 3); // Red cards are more severe
    }

    private TeamComparisonDTO.Prediction predictMatchOutcome(TeamComparisonDTO.ComparisonMetrics metrics, String team1Name, String team2Name) {
        double team1Strength = metrics.getTeam1OverallRating();
        double team2Strength = metrics.getTeam2OverallRating();

        double diff = team1Strength - team2Strength;

        // More realistic formula based on rating difference
        double baseWinProb = 0.30;
        double team1WinProb = baseWinProb + (diff * 0.1);
        double team2WinProb = baseWinProb - (diff * 0.1);
        double drawProb = 0.40; // Higher probability for draw

        // Ensure minimum values
        team1WinProb = Math.max(team1WinProb, 0.1);
        team2WinProb = Math.max(team2WinProb, 0.1);

        // Normalize to 100%
        double total = team1WinProb + team2WinProb + drawProb;
        team1WinProb = (team1WinProb / total) * 100;
        team2WinProb = (team2WinProb / total) * 100;
        drawProb = (drawProb / total) * 100;

        TeamComparisonDTO.Prediction prediction = new TeamComparisonDTO.Prediction();
        prediction.setTeam1Wins((int) Math.round(team1WinProb));
        prediction.setTeam2Wins((int) Math.round(team2WinProb));
        prediction.setDraws((int) Math.round(drawProb));

        // Determine favored team
        if (team1WinProb > team2WinProb && team1WinProb > drawProb) {
            prediction.setFavoredTeam(team1Name);
        } else if (team2WinProb > team1WinProb && team2WinProb > drawProb) {
            prediction.setFavoredTeam(team2Name);
        } else {
            prediction.setFavoredTeam("DRAW_FAVORED");
        }

        return prediction;
    }

    private String generateWinnerMessage(TeamComparisonDTO.Prediction prediction, TeamComparisonDTO.ComparisonMetrics metrics, String team1Name, String team2Name) {
        String favoredTeam = prediction.getFavoredTeam();

        if ("DRAW_FAVORED".equals(favoredTeam)) {
            return "CLOSE_MATCH - Potential Draw";
        }

        double winnerRating = favoredTeam.equals(team1Name) ?
                metrics.getTeam1OverallRating() : metrics.getTeam2OverallRating();

        double winProbability = favoredTeam.equals(team1Name) ?
                prediction.getTeam1Wins() : prediction.getTeam2Wins();

        if (winProbability >= 60) {
            return String.format("CLEAR_VICTORY_%s (%.1f rating)", favoredTeam, winnerRating);
        } else if (winProbability >= 45) {
            return String.format("SLIGHT_ADVANTAGE_%s (%.1f rating)", favoredTeam, winnerRating);
        } else {
            return String.format("MINOR_ADVANTAGE_%s (%.1f rating)", favoredTeam, winnerRating);
        }
    }

    private double calculateConfidence(TeamComparisonDTO.ComparisonMetrics metrics) {
        double ratingDiff = Math.abs(metrics.getTeam1OverallRating() - metrics.getTeam2OverallRating());

        // More difference = more confidence
        double baseConfidence = Math.min(ratingDiff / 2.0, 1.0);

        // Adjust by overall quality (higher rated teams generate more confidence)
        double avgRating = (metrics.getTeam1OverallRating() + metrics.getTeam2OverallRating()) / 2;
        double ratingBonus = Math.max(0, (avgRating - 6.0) / 4.0);

        return Math.min(baseConfidence + ratingBonus, 1.0);
    }

    private double parseDouble(String value, double defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            String cleaned = value.replace("%", "")
                    .replace(",", ".")
                    .replace("'", "")
                    .trim();

            if (cleaned.isEmpty()) {
                return defaultValue;
            }

            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}