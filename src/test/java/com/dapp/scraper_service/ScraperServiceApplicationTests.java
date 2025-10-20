package com.dapp.scraper_service;

import com.dapp.scraper_service.model.dto.PlayerDTO;
import com.dapp.scraper_service.service.DataIntegrationService;
import com.dapp.scraper_service.service.PerformanceCalculatorService;
import com.dapp.scraper_service.service.PlayerService;
import com.dapp.scraper_service.service.PredictionService;
import com.dapp.scraper_service.service.TeamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.dapp.scraper_service.model.MatchStatistics;
import com.dapp.scraper_service.model.PerformanceMetrics;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.ArgumentMatchers.anyList;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("testing") // Activa el perfil 'testing' para usar la base de datos en memoria
class ScraperServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc; // MockMvc para simular peticiones HTTP

	// Mockeamos todos los servicios para aislar la capa de controladores
	@MockBean
	private PlayerService playerService;
	@MockBean
	private TeamService teamService;
	@MockBean
	private PredictionService predictionService;
	@MockBean
	private PerformanceCalculatorService performanceCalculator;
	@MockBean
	private DataIntegrationService dataIntegrationService;

	@Test
	@DisplayName("El contexto de la aplicación debe cargar correctamente")
	void contextLoads() {
		// Esta prueba básica asegura que el contexto de la aplicación se carga
		// correctamente.
		// Si falla, hay un problema en la configuración general de tu aplicación.
	}

	// --- Tests para ScraperController ---

	@Test
	@DisplayName("GET /api/scrape/player debe devolver 200 OK cuando el jugador es encontrado")
	void whenScrapePlayerIsCalled_withValidPlayer_thenReturnsOk() throws Exception {
		// Arrange: Configuramos el mock para que devuelva una lista con un DTO
		PlayerDTO mockPlayer = new PlayerDTO();
		mockPlayer.setName("Lionel Messi");
		when(playerService.getPlayerInfoByName("Lionel Messi")).thenReturn(List.of(mockPlayer));

		// Act & Assert
		mockMvc.perform(get("/api/scrape/player").param("playerName", "Lionel Messi"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Lionel Messi"));
	}

	@Test
	@DisplayName("GET /api/scrape/player debe devolver 404 Not Found cuando el jugador no es encontrado")
	void whenScrapePlayerIsCalled_withInvalidPlayer_thenReturnsNotFound() throws Exception {
		// Arrange: Configuramos el mock para que lance la excepción que el controlador
		// espera
		when(playerService.getPlayerInfoByName(anyString())).thenThrow(new IllegalArgumentException("Not found"));

		// Act & Assert
		mockMvc.perform(get("/api/scrape/player").param("playerName", "Jugador Inexistente"))
				.andExpect(status().isNotFound());
	}

	// --- Tests para AnalysisController ---

	@Test
	@DisplayName("GET /api/analysis/{player}/metrics debe devolver 200 OK con datos válidos")
	void whenGetMetricsIsCalled_withValidPlayer_thenReturnsOk() throws Exception {
		// Arrange: Simulamos que se encuentra al menos una estadística
		when(dataIntegrationService.convertToMatchStatistics(anyString())).thenReturn(List.of(new MatchStatistics()));
		// Y que el calculador devuelve un objeto de métricas
		when(performanceCalculator.calculateMetrics(anyList())).thenReturn(new PerformanceMetrics());

		// Act & Assert
		mockMvc.perform(get("/api/analysis/Test Player/metrics"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("POST /api/analysis/{player}/convert-data debe devolver 200 OK")
	void whenConvertDataIsCalled_thenReturnsOk() throws Exception {
		// Arrange
		when(dataIntegrationService.convertToMatchStatistics(anyString())).thenReturn(Collections.emptyList());

		// Act & Assert
		mockMvc.perform(post("/api/analysis/Test Player/convert-data"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUCCESS"));
	}

}
