package com.dapp.scraper_service.web;

import com.dapp.scraper_service.model.dto.PlayerDTO;
import java.util.List;
import com.dapp.scraper_service.model.dto.TeamDTO;
import com.dapp.scraper_service.service.PlayerService;
import com.dapp.scraper_service.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scrape")
public class ScraperController {

    private final PlayerService playerService;
    private final TeamService teamService;

    public ScraperController(PlayerService playerService, TeamService teamService) {
        this.playerService = playerService;
        this.teamService = teamService;
    }

    @GetMapping("/player")
    public ResponseEntity<List<PlayerDTO>> scrapePlayer(@RequestParam("playerName") String playerName) {
        try {
            List<PlayerDTO> player = playerService.getPlayerInfoByName(playerName);
            return ResponseEntity.ok(player);
        } catch (IllegalArgumentException e) {
            // Si el PlayerService lanza IllegalArgumentException (ej. jugador no
            // encontrado)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            // Para cualquier otro error inesperado durante el scraping
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/team")
    public ResponseEntity<List<TeamDTO>> scrapeTeam(@RequestParam("teamName") String teamName) {
        try {
            List<TeamDTO> team = teamService.getTeamInfoByName(teamName);
            return ResponseEntity.ok(team);
        } catch (IllegalArgumentException e) {
            // Si el TeamService lanza IllegalArgumentException (ej. equipo no encontrado)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            // Para cualquier otro error inesperado durante el scraping
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}