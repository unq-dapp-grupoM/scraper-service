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
@Table(name = "match_statistics")
public class MatchStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerName;
    private String opponent;
    private LocalDate matchDate;
    private String result; // (H)ome, (A)way
    private String position; // FW, MF, DF, etc.

    // BASIC STATISTICS
    private Integer minutesPlayed;
    private Integer goals;
    private Integer assists;
    private Integer yellowCards;
    private Integer redCards;

    // DETAILED STATISTICS
    private Integer shots;
    private Double passAccuracy; // %
    private Integer aerialDuels;
    private Double rating;

    // METADATA
    private String league;
    private String season;

    public MatchStatistics() {
    }

    public MatchStatistics(String playerName, String opponent, LocalDate matchDate) {
        this.playerName = playerName;
        this.opponent = opponent;
        this.matchDate = matchDate;
    }
}