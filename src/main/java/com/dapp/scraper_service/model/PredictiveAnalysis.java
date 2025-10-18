package com.dapp.scraper_service.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "predictive_analysis")
public class PredictiveAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerName;
    private LocalDate analysisDate;

    // PREDICTIVE PROBABILITIES
    private Double goalProbability;
    private Double assistProbability;
    private Double highRatingProbability; // > 7.5
    private Double fullMatchProbability; // 90 mins

    // INFLUENCE FACTORS
    private Double homeAdvantageFactor; // Home vs Away performance
    private Double opponentFactor; // Performance vs specific opponent
    private Double positionFactor; // Performance in specific position
    private Double trendFactor; // Recent momentum

    // RECOMMENDATIONS
    private String performancePrediction; // HIGH, MEDIUM, LOW
    private Double predictiveScore; // 0-100

    public PredictiveAnalysis() {
    }

    public PredictiveAnalysis(String playerName) {
        this.playerName = playerName;
        this.analysisDate = LocalDate.now();
    }
}
