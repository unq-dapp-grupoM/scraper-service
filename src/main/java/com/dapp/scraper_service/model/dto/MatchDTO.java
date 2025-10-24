package com.dapp.scraper_service.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchDTO {
    private String homeTeam;
    private String awayTeam;
    private String date;
    private String competition;
}