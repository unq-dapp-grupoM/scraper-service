package com.dapp.scraper_service.e2e;

import com.dapp.scraper_service.ScraperServiceApplication;
import com.dapp.scraper_service.model.*;
import com.dapp.scraper_service.repository.*;
import com.dapp.scraper_service.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ScraperServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:predictiontest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.web-application-type=none",
        "spring.cache.type=none",
        "scraper.api.key=test-key-123",
        "scraper.timeout=30000",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false"
})
@Transactional
class FunctionalPredictionE2ETest {

    @Autowired
    private DataIntegrationService dataIntegrationService;

    @Autowired
    private PerformanceCalculatorService performanceCalculatorService;

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private MatchStatisticsRepository matchStatisticsRepository;

    @Autowired
    private PerformanceMetricsRepository performanceMetricsRepository;

    @Autowired
    private PredictiveAnalysisRepository predictiveAnalysisRepository;

    @BeforeEach
    void setUp() {
        predictiveAnalysisRepository.deleteAll();
        performanceMetricsRepository.deleteAll();
        matchStatisticsRepository.deleteAll();
        playerRepository.deleteAll();
    }

    @Test
    @DisplayName("E2E: Flujo completo de predicción con datos reales")
    void testCompletePredictionFlow() {
        Player player = createTestPlayer();
        Player savedPlayer = playerRepository.save(player);
        assertNotNull(savedPlayer.getId());

        initializeLazyCollections(savedPlayer);

        List<MatchStatistics> matchStats = dataIntegrationService.convertToMatchStatistics("Test Player");
        assertNotNull(matchStats);
        assertEquals(2, matchStats.size());

        PerformanceMetrics metrics = performanceCalculatorService.calculateMetrics(matchStats);
        assertNotNull(metrics);
        assertEquals("Test Player", metrics.getPlayerName());
        assertTrue(metrics.getAverageRating() > 0);

        PredictiveAnalysis prediction = predictionService.predictPerformance(
                "Test Player",
                "Real Madrid",
                true,
                "FW");

        assertNotNull(prediction);
        assertEquals("Test Player", prediction.getPlayerName());
        assertTrue(prediction.getPredictiveScore() >= 0);
        assertTrue(prediction.getPredictiveScore() <= 100);

        List<PredictiveAnalysis> savedPredictions = predictiveAnalysisRepository.findAll();
        assertEquals(1, savedPredictions.size());
    }

    @Test
    @DisplayName("E2E: Jugador sin datos históricos")
    void testPlayerWithNoHistoricalData() {

        PredictiveAnalysis prediction = predictionService.predictPerformance(
                "NonExistent Player",
                "Barcelona",
                false,
                "MF");

        assertNotNull(prediction);
        assertEquals("NonExistent Player", prediction.getPlayerName());
        assertTrue(prediction.getPredictiveScore() < 50);
    }

    @Test
    @DisplayName("E2E: Cálculo de métricas con datos variados")
    void testMetricsCalculation() {
        Player player = createVariedTestPlayer();
        Player savedPlayer = playerRepository.save(player);

        initializeLazyCollections(savedPlayer);

        List<MatchStatistics> matchStats = dataIntegrationService.convertToMatchStatistics("Varied Player");
        PerformanceMetrics metrics = performanceCalculatorService.calculateMetrics(matchStats);

        assertNotNull(metrics);
        assertEquals("Varied Player", metrics.getPlayerName());
        assertTrue(metrics.getGoalsPerMatch() > 0);
        assertTrue(metrics.getAssistsPerMatch() >= 0);
        assertTrue(metrics.getAverageRating() > 0);
    }

    private void initializeLazyCollections(Player player) {
        if (player.getMatchStats() != null) {
            player.getMatchStats().size();
        }
    }

    private Player createTestPlayer() {
        Player player = new Player();
        player.setName("Test Player");
        player.setCurrentTeam("Test FC");
        player.setAge("25");
        player.setNationality("Testland");
        player.setPositions("FW");

        PlayerMatchStats stats1 = new PlayerMatchStats();
        stats1.setOpponent("Team A");
        stats1.setDate("15/09/2024");
        stats1.setScore("2-1");
        stats1.setPosition("FW");
        stats1.setMinsPlayed("90");
        stats1.setGoals("1");
        stats1.setAssists("1");
        stats1.setShots("4");
        stats1.setPassSuccess("85.5");
        stats1.setAerialsWon("3");
        stats1.setRating("8.2");
        stats1.setPlayer(player);

        PlayerMatchStats stats2 = new PlayerMatchStats();
        stats2.setOpponent("Team B");
        stats2.setDate("08/09/2024");
        stats2.setScore("1-1");
        stats2.setPosition("FW");
        stats2.setMinsPlayed("85");
        stats2.setGoals("0");
        stats2.setAssists("1");
        stats2.setShots("2");
        stats2.setPassSuccess("78.0");
        stats2.setAerialsWon("1");
        stats2.setRating("7.1");
        stats2.setPlayer(player);

        player.setMatchStats(List.of(stats1, stats2));
        return player;
    }

    private Player createVariedTestPlayer() {
        Player player = new Player();
        player.setName("Varied Player");
        player.setCurrentTeam("Varied FC");
        player.setAge("28");
        player.setNationality("Variedland");
        player.setPositions("MF,FW");

        PlayerMatchStats stats = new PlayerMatchStats();
        stats.setOpponent("Strong Team");
        stats.setDate("20/09/2024");
        stats.setScore("3-0");
        stats.setPosition("MF");
        stats.setMinsPlayed("90");
        stats.setGoals("2");
        stats.setAssists("0");
        stats.setShots("5");
        stats.setPassSuccess("92.0");
        stats.setAerialsWon("2");
        stats.setRating("9.0");
        stats.setPlayer(player);

        player.setMatchStats(List.of(stats));
        return player;
    }
}