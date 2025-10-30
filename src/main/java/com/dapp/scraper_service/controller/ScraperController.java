package com.dapp.scraper_service.controller;

import com.dapp.scraper_service.model.Role;
import com.dapp.scraper_service.model.User;
import com.dapp.scraper_service.model.dto.MatchDTO;
import com.dapp.scraper_service.model.dto.PlayerDTO;
import java.util.List;
import java.util.Optional;

import com.dapp.scraper_service.model.dto.TeamDTO;
import com.dapp.scraper_service.model.dto.UserRegistrationRequest;
import com.dapp.scraper_service.model.dto.UserValidationRequest;
import com.dapp.scraper_service.model.dto.UserValidationResponse;
import com.dapp.scraper_service.service.PlayerService;
import com.dapp.scraper_service.service.TeamService;
import com.dapp.scraper_service.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scrape")
public class ScraperController {

    private final PlayerService playerService;
    private final TeamService teamService;
    private final UserService userService;

    public ScraperController(PlayerService playerService, TeamService teamService, UserService userService) {
        this.playerService = playerService;
        this.teamService = teamService;
        this.userService = userService;
    }

    @GetMapping("/player")
    public ResponseEntity<List<PlayerDTO>> scrapePlayer(@RequestParam("playerName") String playerName) {
        try {
            List<PlayerDTO> player = playerService.getPlayerInfoByName(playerName);
            return ResponseEntity.ok(player);
        } catch (IllegalArgumentException e) {
            // Player not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/team")
    public ResponseEntity<List<TeamDTO>> scrapeTeam(@RequestParam("teamName") String teamName) {
        try {
            List<TeamDTO> team = teamService.getTeamInfoByName(teamName);
            return ResponseEntity.ok(team);
        } catch (IllegalArgumentException e) {
            // Team not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/futureMatches")
    public ResponseEntity<List<MatchDTO>> scrapeFutureMatches(@RequestParam("teamName") String teamName) {
        try {
            List<MatchDTO> matches = teamService.getFutureMatchesByTeamName(teamName);
            return ResponseEntity.ok(matches);
        } catch (IllegalArgumentException e) {
            // Si el TeamService lanza IllegalArgumentException (ej. equipo no encontrado)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            // Para cualquier otro error inesperado durante el scraping
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/auth/register")
    public ResponseEntity<String> registerUser(@RequestBody UserRegistrationRequest request) {
        try {
            User user = userService.registerUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getRole() != null ? request.getRole() : Role.USER);
            return ResponseEntity.ok("User registered successfully with ID: " + user.getId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/auth/validate")
    public ResponseEntity<UserValidationResponse> validateUser(@RequestBody UserValidationRequest request) {
        try {
            boolean isValid = userService.validateUser(request.getEmail(), request.getPassword());

            if (isValid) {
                Optional<User> user = userService.findByEmail(request.getEmail());
                if (user.isPresent()) {
                    UserValidationResponse response = UserValidationResponse.builder()
                            .valid(true)
                            .userId(user.get().getId())
                            .email(user.get().getEmail())
                            .role(user.get().getRole())
                            .build();
                    return ResponseEntity.ok(response);
                }
            }

            UserValidationResponse response = UserValidationResponse.builder()
                    .valid(false)
                    .build();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}