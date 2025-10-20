package com.dapp.scraper_service;

import com.dapp.scraper_service.model.Player;
import com.dapp.scraper_service.model.PlayerMatchStats;
import com.dapp.scraper_service.model.dto.PlayerDTO;
import com.dapp.scraper_service.repository.PlayerRepository;
import com.dapp.scraper_service.service.PlayerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    // Usamos @Spy para poder mockear el método getHtmlContent de la clase base
    // mientras probamos la lógica real de PlayerService.
    @Spy
    @InjectMocks
    private PlayerService playerService;

    private Player testPlayer;
    private final String playerName = "Test Player";

    @BeforeEach
    void setUp() {
        testPlayer = new Player();
        testPlayer.setName(playerName);
        testPlayer.setCurrentTeam("Test FC");
        testPlayer.setAge("25");
        testPlayer.setNationality("Testland");
        testPlayer.setPositions("Delantero");
        testPlayer.setMatchStats(new ArrayList<>());

        PlayerMatchStats stats = new PlayerMatchStats();
        stats.setPlayer(testPlayer);
        stats.setOpponent("Opponent FC");
        testPlayer.getMatchStats().add(stats);
    }

    @Test
    @DisplayName("Debe devolver DTOs desde la BD si el jugador ya existe")
    void whenPlayerFoundInDatabase_thenReturnDtoFromDb() {
        // Arrange: Simulamos que el repositorio encuentra al jugador
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(List.of(testPlayer));

        // Act
        List<PlayerDTO> result = playerService.getPlayerInfoByName(playerName);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(playerName, result.get(0).getName());
        assertEquals("Test FC", result.get(0).getCurrentTeam());
        assertEquals(1, result.get(0).getMatchStats().size());

        // Verificamos que no se hizo ninguna llamada de scraping
        verify(playerService, never()).getHtmlContent(anyString());
        verify(playerService, never()).getHtmlContent(anyString(), anyString());
        // Verificamos que no se intentó guardar nada
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("Debe lanzar una excepción si el jugador no se encuentra en la búsqueda")
    void whenPlayerNotFoundInSearch_thenThrowException() {
        // Arrange
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(Collections.emptyList());

        // Simulamos que la búsqueda no devuelve HTML, lo que hará que el parseo falle
        // y se lance la excepción esperada.
        doReturn(null).when(playerService).getHtmlContent(contains("search"), eq(playerName));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            playerService.getPlayerInfoByName(playerName);
        });

        assertFalse(exception.getMessage().contains("Player with name '" + playerName + "' not found in search."));
    }

    @Test
    @DisplayName("Debe guardar al jugador sin estadísticas si el enlace no se encuentra")
    void whenStatsLinkNotFound_thenSavePlayerWithEmptyStats() {
        // Arrange
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(Collections.emptyList());

        String searchHtml = "<html><body><div class='search-result'><h2>Jugadores</h2><table><tbody>" +
                "<tr><td></td></tr>" +
                "<tr><td><a href='/Players/123/Show/Test-Player'>Test Player</a></td></tr>" +
                "</tbody></table></div></body></html>";
        // HTML de resumen con la estructura correcta pero sin el enlace a "Estadísticas
        // del Partido"
        String summaryHtmlNoStatsLink = "<html><body>" +
                "<div class='col12-lg-10 col12-m-10 col12-s-9 col12-xs-8'>" +
                "  <div class='col12-lg-6'><span class='info-label'>Nombre:</span> Test Player</div>" +
                "  <div class='col12-lg-6'><span class='info-label'>Equipo Actual:</span> Test FC</div>" +
                "</div>" +
                "</body></html>";

        doReturn(searchHtml).when(playerService).getHtmlContent(contains("search"), eq(playerName));
        doReturn(summaryHtmlNoStatsLink).when(playerService).getHtmlContent(contains("/Players/123/Show/"));

        // Act
        List<PlayerDTO> result = playerService.getPlayerInfoByName(playerName);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        // Verificamos que la lista de estadísticas está vacía
        assertTrue(result.get(0).getMatchStats().isEmpty());

        // Verificamos que la llamada a la página de estadísticas nunca ocurrió
        verify(playerService, never()).getHtmlContent(contains("/History/"));

        // Verificamos que aun así se guardó el jugador
        verify(playerRepository, times(1)).save(any(Player.class));
    }

    @Test
    @DisplayName("Debe lanzar una RuntimeException si el scraping falla inesperadamente")
    void whenScrapingFails_thenThrowRuntimeException() {
        // Arrange
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(Collections.emptyList());

        // Simulamos que la llamada de red falla
        doThrow(new RuntimeException("Network Error"))
                .when(playerService).getHtmlContent(anyString(), anyString());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            playerService.getPlayerInfoByName(playerName);
        });

        assertEquals("An unexpected error occurred while fetching player data.", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Network Error", exception.getCause().getMessage());
    }
}