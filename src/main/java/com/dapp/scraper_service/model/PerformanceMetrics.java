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
@Table(name = "performance_metrics")
public class PerformanceMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerName;
    private LocalDate analysisDate;

    // OFFENSIVE METRICS
    private Double goalsPerMatch;
    private Double assistsPerMatch;
    private Double goalInvolvement; // (Goals + Assists) / Matches
    private Double shotsPerMatch;
    private Double shotAccuracy; // Shots on target / Total shots

    // DISTRIBUTION METRICS
    private Double passAccuracy;
    private Double keyPassesPerMatch;

    // DEFENSIVE METRICS
    private Double aerialDuelsWon;
    private Double recoveriesPerMatch;

    // CONSISTENCY METRICS
    private Double averageRating;
    private Double ratingDeviation; // Consistency
    private Double minutesPerMatch;

    // ADVANCED METRICS FOR PREDICTION
    private Double offensiveImpact; // Weighted score
    private Double performanceTrend;
    private Double goalProbability;
    private Double assistProbability;

    public PerformanceMetrics(String playerName) {
        this.playerName = playerName;
        this.analysisDate = LocalDate.now();
    }

    // Constructors, getters and setters
    public PerformanceMetrics() {
    }

    public void setRatingDeviation(Double ratingDeviation) {
        this.ratingDeviation = ratingDeviation;
    }

    public Double getGoalProbability() {
        return goalProbability != null ? goalProbability : 0.0;
    }

    public Double getAssistProbability() {
        return assistProbability != null ? assistProbability : 0.0;
    }

    public Double getAverageRating() {
        return averageRating != null ? averageRating : 6.0;
    }

    public Double getPerformanceTrend() {
        return performanceTrend != null ? performanceTrend : 0.0;
    }

    public Double getRatingDeviation() {
        return ratingDeviation != null ? ratingDeviation : 1.0;
    }

    public Double getOffensiveImpact() {
        return offensiveImpact != null ? offensiveImpact : 0.0;
    }

    // Asegúrate de que todas las métricas tengan valores por defecto
    public Double getGoalsPerMatch() {
        return goalsPerMatch != null ? goalsPerMatch : 0.0;
    }

    public Double getAssistsPerMatch() {
        return assistsPerMatch != null ? assistsPerMatch : 0.0;
    }

    public Double getShotsPerMatch() {
        return shotsPerMatch != null ? shotsPerMatch : 0.0;
    }

    public Double getPassAccuracy() {
        return passAccuracy != null ? passAccuracy : 0.0;
    }
}