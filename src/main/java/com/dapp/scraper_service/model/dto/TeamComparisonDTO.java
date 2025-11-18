package com.dapp.scraper_service.model.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Data
@NoArgsConstructor
@Getter
@Setter
public class TeamComparisonDTO {
    private String teamName1;
    private String teamName2;
    private ComparisonMetrics comparison;
    private String suggestedWinner;
    private double confidenceLevel;

    @Data
    public static class ComparisonMetrics {
        private double team1OverallRating;
        private double team2OverallRating;
        private Map<String, Double> positionalAdvantages;
        private Map<String, Double> keyMetricsComparison;
        private Prediction prediction;
    }

    @Data
    public static class Prediction {
        private int team1Wins;
        private int team2Wins;
        private int draws;
        private String favoredTeam;
    }
}