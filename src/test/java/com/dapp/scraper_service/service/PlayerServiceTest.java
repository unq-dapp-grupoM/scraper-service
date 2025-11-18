package com.dapp.scraper_service.service;

import com.dapp.scraper_service.model.Player;
import com.dapp.scraper_service.model.PlayerMatchStats;
import com.dapp.scraper_service.model.dto.PlayerDTO;
import com.dapp.scraper_service.repository.PlayerRepository;

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

    // We use @Spy to be able to mock the getHtmlContent method of the base class
    // while testing the actual logic of PlayerService.
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
    @DisplayName("Should return DTOs from the DB if the player already exists")
    void whenPlayerFoundInDatabase_thenReturnDtoFromDb() {
        // Arrange: We simulate that the repository finds the player
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(List.of(testPlayer));

        // Act
        List<PlayerDTO> result = playerService.getPlayerInfoByName(playerName);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(playerName, result.get(0).getName());
        assertEquals("Test FC", result.get(0).getCurrentTeam());
        assertEquals(1, result.get(0).getMatchStats().size());

        // We verify that no scraping call was made
        verify(playerService, never()).getHtmlContent(anyString());
        verify(playerService, never()).getHtmlContent(anyString(), anyString());
        // We verify that nothing was attempted to be saved
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("Should throw an exception if the player is not found in the search")
    void whenPlayerNotFoundInSearch_thenThrowException() {
        // Arrange
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(Collections.emptyList());

        // We simulate that the search does not return HTML, which will cause parsing to
        // fail
        // and the expected exception to be thrown.
        doReturn(null).when(playerService).getHtmlContent(contains("search"), eq(playerName));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            playerService.getPlayerInfoByName(playerName);
        });

        assertFalse(exception.getMessage().contains("Player with name '" + playerName + "' not found in search."));
    }

    @Test
    @DisplayName("Should save the player without stats if the link is not found")
    void whenStatsLinkNotFound_thenSavePlayerWithEmptyStats() {
        // Arrange
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(Collections.emptyList());

        String searchHtml = "<html><body><div class='search-result'><h2>Jugadores</h2><table><tbody>" +
                "<tr><td></td></tr>" +
                "<tr><td><a href='/Players/123/Show/Test-Player'>Test Player</a></td></tr>" +
                "</tbody></table></div></body></html>";
        // Summary HTML with the correct structure but without the link to "Match
        // Statistics"
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
        // We verify that the stats list is empty
        assertTrue(result.get(0).getMatchStats().isEmpty());

        // We verify that the call to the stats page never occurred
        verify(playerService, never()).getHtmlContent(contains("/History/"));

        // We verify that the player was saved anyway
        verify(playerRepository, times(1)).save(any(Player.class));
    }

    @Test
    @DisplayName("Should throw a RuntimeException if scraping fails unexpectedly")
    void whenScrapingFails_thenThrowRuntimeException() {
        // Arrange
        when(playerRepository.findByNameContainingIgnoreCase(playerName)).thenReturn(Collections.emptyList());

        // We simulate that the network call fails
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