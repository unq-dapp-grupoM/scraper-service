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
        // Configuración de datos de prueba que se usarán en varios tests
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
        // Arrange: Configuramos el mock para que no encuentre al jugador
        when(playerRepository.findByNameContainingIgnoreCase("NonExistent Player")).thenReturn(Collections.emptyList());

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(RuntimeException.class, () -> {
            dataIntegrationService.convertToMatchStatistics("NonExistent Player");
        });

        assertEquals("Player not found: NonExistent Player", exception.getMessage());
    }

    @Test
    void whenStatsAlreadyExist_thenReturnExistingStats() {
        // Arrange: Simulamos que el jugador y sus estadísticas ya existen
        String playerName = "Existing Player";
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(List.of(new Player()));

        List<MatchStatistics> existingStats = List.of(new MatchStatistics());
        when(matchStatisticsRepository.findByPlayerName(playerName)).thenReturn(existingStats);

        // Act: Llamamos al método
        List<MatchStatistics> result = dataIntegrationService.convertToMatchStatistics(playerName);

        // Assert: Verificamos que devuelve los datos existentes y no intenta guardar de
        // nuevo
        assertEquals(1, result.size());
        assertSame(existingStats, result);
        verify(matchStatisticsRepository, never()).saveAll(any());
    }

    @Test
    void whenNewPlayerStats_thenConvertAndSave() {
        // Arrange: Simulamos un jugador nuevo sin estadísticas pre-convertidas
        String playerName = "Test Player";
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(List.of(testPlayer));
        when(matchStatisticsRepository.findByPlayerName(playerName)).thenReturn(Collections.emptyList());

        // Act: Llamamos al método
        List<MatchStatistics> result = dataIntegrationService.convertToMatchStatistics(playerName);

        // Assert: Verificamos que la conversión es correcta
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

        // Verificamos que se intentó guardar el resultado en el repositorio
        verify(matchStatisticsRepository, times(1)).saveAll(result);
    }

    @Test
    void whenParsingFails_thenUseDefaultValues() {
        // Arrange: Usamos datos malformados
        scrapedStat.setDate("invalid-date");
        scrapedStat.setMinsPlayed("N/A");
        scrapedStat.setPassSuccess("error");
        scrapedStat.setRating(null);

        String playerName = "Test Player";
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(List.of(testPlayer));
        when(matchStatisticsRepository.findByPlayerName(playerName)).thenReturn(Collections.emptyList());

        // Act
        List<MatchStatistics> result = dataIntegrationService.convertToMatchStatistics(playerName);

        // Assert: Verificamos que se usaron los valores por defecto
        assertEquals(1, result.size());
        MatchStatistics convertedStat = result.get(0);

        assertEquals(LocalDate.now(), convertedStat.getMatchDate()); // Fallback de fecha
        assertEquals(0, convertedStat.getMinutesPlayed()); // Fallback de integer
        assertEquals(0.0, convertedStat.getPassAccuracy()); // Fallback de double
        assertEquals(0.0, convertedStat.getRating()); // Fallback de double para null
        assertEquals("2025-2026", convertedStat.getSeason()); // Fallback de temporada

        verify(matchStatisticsRepository, times(1)).saveAll(any());
    }
}