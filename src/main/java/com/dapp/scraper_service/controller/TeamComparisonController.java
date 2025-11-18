package com.dapp.scraper_service.controller;

import com.dapp.scraper_service.audit.AuditQuery;
import com.dapp.scraper_service.model.QueryType;
import com.dapp.scraper_service.model.dto.TeamComparisonDTO;
import com.dapp.scraper_service.service.TeamComparisonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scrape/teams")
@CrossOrigin(origins = "*")
public class TeamComparisonController {

    private final TeamComparisonService teamComparisonService;

    public TeamComparisonController(TeamComparisonService teamComparisonService) {
        this.teamComparisonService = teamComparisonService;
    }

    @GetMapping("/compare")
    @AuditQuery(QueryType.TEAM_COMPARISON)
    public ResponseEntity<TeamComparisonDTO> compareTeams(
            @RequestParam String team1,
            @RequestParam String team2) {

        try {
            TeamComparisonDTO comparison = teamComparisonService.compareTeams(team1, team2);
            return ResponseEntity.ok(comparison);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}