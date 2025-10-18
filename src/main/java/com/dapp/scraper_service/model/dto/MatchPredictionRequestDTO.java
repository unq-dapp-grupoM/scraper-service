package com.dapp.scraper_service.model.dto;

import java.util.List;

import lombok.Data;

@Data
public class MatchPredictionRequestDTO {

    private String homeTeam;
    private String awayTeam;
    private List<String> homeKeyPlayers;
    private List<String> awayKeyPlayers;

    public MatchPredictionRequestDTO() {
    }
}
