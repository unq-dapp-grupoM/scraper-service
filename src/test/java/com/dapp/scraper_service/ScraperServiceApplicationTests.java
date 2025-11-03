package com.dapp.scraper_service;

import com.dapp.scraper_service.model.dto.PlayerDTO;
import com.dapp.scraper_service.model.PredictiveAnalysis;
import com.dapp.scraper_service.model.User;
import com.dapp.scraper_service.service.DataIntegrationService;
import com.dapp.scraper_service.service.PerformanceCalculatorService;
import com.dapp.scraper_service.service.PlayerService;
import com.dapp.scraper_service.service.PredictionService;
import com.dapp.scraper_service.service.QueryHistoryService;
import com.dapp.scraper_service.service.TeamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.dapp.scraper_service.repository.UserRepository;
import com.dapp.scraper_service.model.MatchStatistics;
import com.dapp.scraper_service.model.PerformanceMetrics;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.ArgumentMatchers.anyList;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("testing") // Activates the 'testing' profile to use the in-memory database
class ScraperServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc; // MockMvc to simulate HTTP requests

	// We mock all services to isolate the controller layer
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
	@MockBean
	private QueryHistoryService queryHistoryService;
	@MockBean
	private UserRepository userRepository; // Needed by QueryHistoryService

	@Test
	@DisplayName("GET /api/scrape/player should return 200 OK when the player is found")
	void whenScrapePlayerIsCalled_withValidPlayer_thenReturnsOk() throws Exception {
		// Arrange: We configure the mock to return a list with a DTO
		PlayerDTO mockPlayer = new PlayerDTO();
		mockPlayer.setName("Lionel Messi");
		when(playerService.getPlayerInfoByName("Lionel Messi")).thenReturn(List.of(mockPlayer));

		// Act & Assert
		mockMvc.perform(get("/api/scrape/player").param("playerName", "Lionel Messi"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Lionel Messi"));
	}

	@Test
	@DisplayName("GET /api/scrape/player should return 404 Not Found when the player is not found")
	void whenScrapePlayerIsCalled_withInvalidPlayer_thenReturnsNotFound() throws Exception {
		// Arrange: We configure the mock to throw the exception that the controller
		// expects
		when(playerService.getPlayerInfoByName(anyString())).thenThrow(new IllegalArgumentException("Not found"));

		// Act & Assert
		mockMvc.perform(get("/api/scrape/player").param("playerName", "Jugador Inexistente"))
				.andExpect(status().isNotFound());
	}

	// --- Tests for AnalysisController ---

	@Test
	@DisplayName("GET /api/analysis/{player}/performanceMetrics should return 200 OK with valid data")
	void whenGetMetricsIsCalled_withValidPlayer_thenReturnsOk() throws Exception {
		// Arrange: We simulate that at least one statistic is found
		when(dataIntegrationService.convertToMatchStatistics(anyString())).thenReturn(List.of(new MatchStatistics()));
		// And that the calculator returns a metrics object
		when(performanceCalculator.calculateMetrics(anyList())).thenReturn(new PerformanceMetrics());
		// Mock user lookup for history
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));

		// Act & Assert
		mockMvc.perform(get("/api/analysis/Test Player/performanceMetrics")
				.param("userEmail", "test@example.com"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("GET /api/analysis/{player}/prediction should return 200 OK with valid data")
	void whenPredictPerformanceIsCalled_withValidPlayer_thenReturnsOk() throws Exception {
		// Arrange
		when(dataIntegrationService.convertToMatchStatistics(anyString())).thenReturn(List.of(new MatchStatistics()));
		when(predictionService.predictPerformance(anyString(), anyString(), anyBoolean(), anyString()))
				.thenReturn(new PredictiveAnalysis());
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));

		// Act & Assert
		mockMvc.perform(get("/api/analysis/Test Player/prediction")
				.param("userEmail", "test@example.com")
				.param("opponent", "Some Team")
				.param("isHome", "true")
				.param("position", "FW"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("GET /api/analysis/{player}/comparison should return 200 OK with valid data")
	void whenGetComparativeAnalysisIsCalled_withValidPlayer_thenReturnsOk() throws Exception {
		// Arrange
		when(dataIntegrationService.convertToMatchStatistics(anyString())).thenReturn(List.of(new MatchStatistics()));
		when(performanceCalculator.calculateMetrics(anyList())).thenReturn(new PerformanceMetrics());
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));

		// Act & Assert
		mockMvc.perform(get("/api/analysis/Test Player/comparison")
				.param("userEmail", "test@example.com"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUCCESS"));
	}

	@Test
	@DisplayName("GET /api/analysis/history/{player} should return 200 OK")
	void whenGetQueryHistoryIsCalled_withValidParams_thenReturnsOk() throws Exception {
		// Arrange
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));
		when(queryHistoryService.getHistory(anyString(), any(), anyString())).thenReturn(List.of());

		// Act & Assert
		mockMvc.perform(get("/api/analysis/history/Test Player")
				.param("date", "28-11-2023")
				.param("userEmail", "test@example.com"))
				.andExpect(status().isOk());
	}
}
