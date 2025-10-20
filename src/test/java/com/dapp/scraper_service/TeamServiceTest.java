package com.dapp.scraper_service;

import com.dapp.scraper_service.model.Team;
import com.dapp.scraper_service.model.TeamPlayer;
import com.dapp.scraper_service.model.dto.TeamDTO;
import com.dapp.scraper_service.repository.TeamRepository;
import com.dapp.scraper_service.service.TeamService;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Spy
    @InjectMocks
    private TeamService teamService;

    private Team testTeam;
    private final String teamName = "Test FC";

    @BeforeEach
    void setUp() {
        testTeam = new Team();
        testTeam.setName(teamName);
        testTeam.setSquad(new ArrayList<>());

        TeamPlayer player = new TeamPlayer();
        player.setName("Test Player");
        player.setAge("25");
        player.setPosition("FW");
        player.setTeam(testTeam);
        testTeam.getSquad().add(player);
    }

    @Test
    @DisplayName("Debe devolver DTOs desde la BD si el equipo ya existe")
    void whenTeamFoundInDatabase_thenReturnDtoFromDb() {
        // Arrange: Simulamos que el repositorio encuentra al equipo
        when(teamRepository.findByNameContainingIgnoreCase(teamName)).thenReturn(List.of(testTeam));

        // Act
        List<TeamDTO> result = teamService.getTeamInfoByName(teamName);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(teamName, result.get(0).getName());
        assertEquals(1, result.get(0).getSquad().size());
        assertEquals("Test Player", result.get(0).getSquad().get(0).getName());

        // Verificamos que no se hizo ninguna llamada de scraping ni se guardó nada
        verify(teamService, never()).getHtmlContent(anyString(), anyString());
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("Debe lanzar una excepción si el equipo no se encuentra en la búsqueda")
    void whenTeamNotFoundInSearch_thenThrowException() {
        // Arrange
        when(teamRepository.findByNameContainingIgnoreCase(teamName)).thenReturn(Collections.emptyList());

        // Simulamos un HTML de búsqueda sin resultados para equipos
        String emptySearchHtml = "<html><body><div class='search-result'><h2>Equipos</h2>" +
                "<table><tbody></tbody></table></div></body></html>";
        doReturn(emptySearchHtml).when(teamService).getHtmlContent(anyString(), anyString());

        // Act & Assert
        // El servicio envuelve las excepciones específicas en una RuntimeException
        // genérica.
        // Verificamos que se lance esta excepción y que la causa sea la que esperamos.
        Exception exception = assertThrows(RuntimeException.class, () -> {
            teamService.getTeamInfoByName(teamName);
        });

        assertTrue(exception.getMessage().contains("An unexpected error occurred while fetching team data."));
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertTrue(
                exception.getCause().getMessage().contains("Team with name '" + teamName + "' not found in search."));
    }

    @Test
    @DisplayName("Debe lanzar una RuntimeException si el scraping falla inesperadamente")
    void whenScrapingFails_thenThrowRuntimeException() {
        // Arrange
        when(teamRepository.findByNameContainingIgnoreCase(teamName)).thenReturn(Collections.emptyList());

        // Simulamos que la llamada de red falla
        doThrow(new RuntimeException("Network Error"))
                .when(teamService).getHtmlContent(anyString(), anyString());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            teamService.getTeamInfoByName(teamName);
        });

        assertEquals("An unexpected error occurred while fetching team data.", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Network Error", exception.getCause().getMessage());
    }
}