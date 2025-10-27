package com.dapp.scraper_service;

import com.dapp.scraper_service.model.MatchStatistics;
import com.dapp.scraper_service.model.Player;
import com.dapp.scraper_service.model.PlayerMatchStats;
import com.dapp.scraper_service.repository.MatchStatisticsRepository;
import com.dapp.scraper_service.repository.PlayerRepository;
import com.dapp.scraper_service.service.DataIntegrationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataIntegrationServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private MatchStatisticsRepository matchStatisticsRepository;

    @InjectMocks
    private DataIntegrationService dataIntegrationService;

    private Player testPlayer;
    private PlayerMatchStats scrapedStat;

    @BeforeEach
    void setUp() {
        // Setup of test data to be used in various tests
        testPlayer = new Player();
        testPlayer.setName("Test Player");

        scrapedStat = new PlayerMatchStats();
        scrapedStat.setOpponent("Opponent FC");
        scrapedStat.setDate("15/09/2024");
        scrapedStat.setScore("2-1");
        scrapedStat.setPosition("Delantero (DC)");
        scrapedStat.setMinsPlayed("90'");
        scrapedStat.setGoals("1");
        scrapedStat.setAssists("0");
        scrapedStat.setYellowCards("1");
        scrapedStat.setRedCards("0");
        scrapedStat.setShots("3");
        scrapedStat.setPassSuccess("85,5%");
        scrapedStat.setAerialsWon("2");
        scrapedStat.setRating("8.1");

        List<PlayerMatchStats> statsList = new ArrayList<>();
        statsList.add(scrapedStat);
        testPlayer.setMatchStats(statsList);
    }

    @Test
    void whenPlayerNotFound_thenThrowRuntimeException() {
        // Arrange: We configure the mock to not find the player
        when(playerRepository.findByNameContainingIgnoreCase("NonExistent Player")).thenReturn(Collections.emptyList());

        // Act & Assert: We verify that the expected exception is thrown
        Exception exception = assertThrows(RuntimeException.class, () -> {
            dataIntegrationService.convertToMatchStatistics("NonExistent Player");
        });

        assertEquals("Player not found: NonExistent Player", exception.getMessage());
    }

    @Test
    void whenStatsAlreadyExist_thenReturnExistingStats() {
        // Arrange: We simulate that the player and their stats already exist
        String playerName = "Existing Player";
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(List.of(new Player()));

        List<MatchStatistics> existingStats = List.of(new MatchStatistics());
        when(matchStatisticsRepository.findByPlayerName(playerName)).thenReturn(existingStats);

        // Act: We call the method
        List<MatchStatistics> result = dataIntegrationService.convertToMatchStatistics(playerName);

        // Assert: We verify that it returns the existing data and does not try to save
        // again
        assertEquals(1, result.size());
        assertSame(existingStats, result);
        verify(matchStatisticsRepository, never()).saveAll(any());
    }

    @Test
    void whenNewPlayerStats_thenConvertAndSave() {
        // Arrange: We simulate a new player without pre-converted stats
        String playerName = "Test Player";
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(List.of(testPlayer));
        when(matchStatisticsRepository.findByPlayerName(playerName)).thenReturn(Collections.emptyList());
        // Act: We call the method
        List<MatchStatistics> result = dataIntegrationService.convertToMatchStatistics(playerName);

        // Assert: We verify that the conversion is correct
        assertNotNull(result);
        assertEquals(1, result.size());

        MatchStatistics convertedStat = result.get(0);
        assertEquals("Test Player", convertedStat.getPlayerName());
        assertEquals("Opponent FC", convertedStat.getOpponent());
        assertEquals(LocalDate.of(2024, 9, 15), convertedStat.getMatchDate());
        assertEquals("FW", convertedStat.getPosition());
        assertEquals(90, convertedStat.getMinutesPlayed());
        assertEquals(1, convertedStat.getGoals());
        assertEquals(0, convertedStat.getAssists());
        assertEquals(1, convertedStat.getYellowCards());
        assertEquals(0, convertedStat.getRedCards());
        assertEquals(3, convertedStat.getShots());
        assertEquals(85.5, convertedStat.getPassAccuracy());
        assertEquals(2, convertedStat.getAerialDuels());
        assertEquals(8.1, convertedStat.getRating());
        assertEquals("2024-2025", convertedStat.getSeason());

        // We verify that an attempt was made to save the result in the repository
        verify(matchStatisticsRepository, times(1)).saveAll(result);
    }

    @Test
    void whenParsingFails_thenUseDefaultValues() {
        // Arrange: We use malformed data
        scrapedStat.setDate("invalid-date");
        scrapedStat.setMinsPlayed("N/A");
        scrapedStat.setPassSuccess("error");
        scrapedStat.setRating(null);

        String playerName = "Test Player";
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(List.of(testPlayer));
        when(matchStatisticsRepository.findByPlayerName(playerName)).thenReturn(Collections.emptyList());

        // Act
        List<MatchStatistics> result = dataIntegrationService.convertToMatchStatistics(playerName);

        // Assert: We verify that the default values were used
        assertEquals(1, result.size());
        MatchStatistics convertedStat = result.get(0);

        assertEquals(LocalDate.now(), convertedStat.getMatchDate()); // Date fallback
        assertEquals(0, convertedStat.getMinutesPlayed()); // Integer fallback
        assertEquals(0.0, convertedStat.getPassAccuracy()); // Double fallback
        assertEquals(0.0, convertedStat.getRating()); // Double fallback for null
        assertEquals("2025-2026", convertedStat.getSeason()); // Season fallback

        verify(matchStatisticsRepository, times(1)).saveAll(any());
    }
}