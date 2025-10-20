package com.dapp.scraper_service.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.dapp.scraper_service.model.MatchStatistics;
import com.dapp.scraper_service.model.Player;
import com.dapp.scraper_service.model.PlayerMatchStats;
import com.dapp.scraper_service.repository.MatchStatisticsRepository;
import com.dapp.scraper_service.repository.PlayerRepository;

@Service
public class DataIntegrationService {

    private final PlayerRepository playerRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;

    public DataIntegrationService(PlayerRepository playerRepository,
            MatchStatisticsRepository matchStatisticsRepository) {
        this.playerRepository = playerRepository;
        this.matchStatisticsRepository = matchStatisticsRepository;
    }

    /**
     * Convierte datos de PlayerMatchStats (scraping) a MatchStatistics (análisis)
     */
    public List<MatchStatistics> convertToMatchStatistics(String playerName) {
        List<Player> players = playerRepository.findByNameContainingIgnoreCase(playerName);
        if (players.isEmpty()) {
            throw new RuntimeException("Player not found: " + playerName);
        }

        Player player = players.get(0);

        // Verificar si ya existen datos convertidos
        List<MatchStatistics> existingStats = matchStatisticsRepository.findByPlayerName(playerName);
        if (!existingStats.isEmpty()) {
            return existingStats;
        }

        List<MatchStatistics> matchStatsList = new ArrayList<>();

        for (PlayerMatchStats scrapedStat : player.getMatchStats()) {
            MatchStatistics analysisStat = convertSingleMatchStat(scrapedStat, player.getName());
            matchStatsList.add(analysisStat);
        }

        matchStatisticsRepository.saveAll(matchStatsList);
        return matchStatsList;
    }

    private MatchStatistics convertSingleMatchStat(PlayerMatchStats scrapedStat, String playerName) {
        MatchStatistics analysisStat = new MatchStatistics();
        analysisStat.setPlayerName(playerName);
        analysisStat.setOpponent(scrapedStat.getOpponent());
        analysisStat.setMatchDate(parseDate(scrapedStat.getDate()));
        analysisStat.setResult(determineResult(scrapedStat.getScore(), scrapedStat.getOpponent()));
        analysisStat.setPosition(convertPosition(scrapedStat.getPosition()));

        // Convertir estadísticas básicas
        analysisStat.setMinutesPlayed(parseInteger(scrapedStat.getMinsPlayed()));
        analysisStat.setGoals(parseInteger(scrapedStat.getGoals()));
        analysisStat.setAssists(parseInteger(scrapedStat.getAssists()));
        analysisStat.setYellowCards(parseInteger(scrapedStat.getYellowCards()));
        analysisStat.setRedCards(parseInteger(scrapedStat.getRedCards()));

        // Convertir estadísticas detalladas
        analysisStat.setShots(parseInteger(scrapedStat.getShots()));
        analysisStat.setPassAccuracy(parseDouble(scrapedStat.getPassSuccess()));
        analysisStat.setAerialDuels(parseInteger(scrapedStat.getAerialsWon()));
        analysisStat.setRating(parseDouble(scrapedStat.getRating()));

        // Metadatos
        analysisStat.setLeague("Unknown"); // Puedes extraer esto del scraping si está disponible
        analysisStat.setSeason(extractSeason(scrapedStat.getDate()));

        return analysisStat;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            // Asumiendo formato "dd/MM/yyyy" o similar
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            return LocalDate.now(); // Fallback
        }
    }

    private String determineResult(String score, String opponent) {
        // Lógica simple para determinar si fue local (H) o visitante (A)
        // Esto depende de cómo estructuren los nombres de oponentes en tu scraping
        return "H"; // Placeholder - implementar lógica real
    }

    private String convertPosition(String position) {
        // Convertir posiciones específicas a formatos estándar
        if (position.contains("DL") || position.contains("Defensa"))
            return "DF";
        if (position.contains("MP") || position.contains("Mediocampista"))
            return "MF";
        if (position.contains("Delantero") || position.contains("FW"))
            return "FW";
        return position;
    }

    private Integer parseInteger(String value) {
        try {
            return value != null ? Integer.parseInt(value.replace("'", "").trim()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Double parseDouble(String value) {
        try {
            return value != null ? Double.parseDouble(value.replace("%", "").replace(",", ".").trim()) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String extractSeason(String dateStr) {
        try {
            LocalDate date = parseDate(dateStr);
            int year = date.getYear();
            // Asumiendo temporada europea (agosto a mayo)
            return date.getMonthValue() >= 8 ? year + "-" + (year + 1) : (year - 1) + "-" + year;
        } catch (Exception e) {
            return "2024";
        }
    }
}
