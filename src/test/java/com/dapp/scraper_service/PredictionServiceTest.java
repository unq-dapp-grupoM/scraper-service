package com.dapp.scraper_service;

import com.dapp.scraper_service.model.MatchStatistics;
import com.dapp.scraper_service.model.PerformanceMetrics;
import com.dapp.scraper_service.model.PredictiveAnalysis;
import com.dapp.scraper_service.repository.MatchStatisticsRepository;
import com.dapp.scraper_service.repository.PredictiveAnalysisRepository;
import com.dapp.scraper_service.service.PerformanceCalculatorService;
import com.dapp.scraper_service.service.PredictionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock
    private PerformanceCalculatorService performanceCalculator;

    @Mock
    private MatchStatisticsRepository matchStatisticsRepository;

    @Mock
    private PredictiveAnalysisRepository predictiveAnalysisRepository;

    @InjectMocks
    private PredictionService predictionService;

    private List<MatchStatistics> historicalMatches;
    private PerformanceMetrics testMetrics;
    private final String playerName = "Test Player";

    @BeforeEach
    void setUp() {
        // Configurar datos de prueba
        historicalMatches = new ArrayList<>();
        historicalMatches.add(createMatch("H", "Opponent A", "FW", 8.0, 90, "2024"));
        historicalMatches.add(createMatch("A", "Opponent B", "FW", 7.0, 90, "2024"));
        historicalMatches.add(createMatch("H", "Opponent A", "MF", 9.0, 80, "2024")); // Menos de 85 mins
        historicalMatches.add(createMatch("A", "Opponent C", "FW", 6.0, 70, "2023")); // Old season

        testMetrics = new PerformanceMetrics();
        testMetrics.setPlayerName(playerName);
        testMetrics.setGoalProbability(0.5);
        testMetrics.setAssistProbability(0.3);
        testMetrics.setAverageRating(8.0);
        testMetrics.setRatingDeviation(0.8);
        testMetrics.setPerformanceTrend(1.2);

        // Configurar mocks
        when(predictiveAnalysisRepository.save(any(PredictiveAnalysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private MatchStatistics createMatch(String result, String opponent, String position, double rating, int minutes,
            String season) {
        MatchStatistics match = new MatchStatistics();
        match.setResult(result);
        match.setOpponent(opponent);
        match.setPosition(position);
        match.setRating(rating);
        match.setMinutesPlayed(minutes);
        match.setSeason(season);
        match.setMatchDate(LocalDate.now());
        return match;
    }

    @Test
    @DisplayName("Debe filtrar datos históricos por la temporada actual")
    void whenPredicting_thenUsesOnlyCurrentSeasonDataForFactors() {
        // Arrange
        when(matchStatisticsRepository.findByPlayerName(playerName)).thenReturn(historicalMatches);
        when(performanceCalculator.calculateMetrics(anyList())).thenReturn(testMetrics);

        // Act
        predictionService.predictPerformance(playerName, "Opponent D", true, "FW");

        // Assert
        // Verificamos que el calculador de métricas recibe solo los 3 partidos de la
        // temporada 2024
        ArgumentCaptor<List<MatchStatistics>> captor = ArgumentCaptor.forClass(List.class);
        verify(performanceCalculator).calculateMetrics(captor.capture());
        assertEquals(3, captor.getValue().size());
        assertTrue(captor.getValue().stream().allMatch(m -> "2024".equals(m.getSeason())));
    }

    @Test
    @DisplayName("Debe calcular todas las predicciones y factores correctamente")
    void whenPredictPerformance_thenCalculatesAllValuesCorrectly() {
        // Arrange
        when(matchStatisticsRepository.findByPlayerName(playerName)).thenReturn(historicalMatches);
        when(performanceCalculator.calculateMetrics(anyList())).thenReturn(testMetrics);

        String opponent = "Opponent A";
        boolean isHome = true;
        String position = "FW";

        // Act
        PredictiveAnalysis result = predictionService.predictPerformance(playerName, opponent, isHome, position);

        // Assert
        assertNotNull(result);
        assertEquals(playerName, result.getPlayerName());

        // Goal Probability: 0.5 (base) * 1.0 (opponent) * 1.1 (home) = 0.55
        assertEquals(0.55, result.getGoalProbability(), 0.001);

        // Assist Probability: 0.3 (base) * 1.0 (opponent) * 1.05 (home) = 0.315
        assertEquals(0.315, result.getAssistProbability(), 0.001);

        // High Rating Probability (z-score = (7.5 - 8.0) / 0.8 = -0.625) -> ~0.73
        assertTrue(result.getHighRatingProbability() > 0.7 && result.getHighRatingProbability() < 0.75);

        // Full Match Probability: 2 de 3 partidos de la temporada 2024 tienen >= 85
        // mins (90, 90, 80)
        assertEquals(2.0 / 3.0, result.getFullMatchProbability(), 0.001);

        // Opponent Factor: Avg vs A (8.5) / Overall Avg (8.0) = 1.0625
        assertEquals(1.0625, result.getOpponentFactor(), 0.001);

        // Position Factor: Avg in FW (7.5) / Overall Avg (8.0) = 0.9375
        assertEquals(0.9375, result.getPositionFactor(), 0.001);

        // Home Advantage: Avg Home (8.5) / Avg Away (7.0) = 1.214
        assertEquals(1.214, result.getHomeAdvantageFactor(), 0.001);

        // Trend Factor
        assertEquals(1.2, result.getTrendFactor());

        // Performance Level
        assertTrue(result.getPredictiveScore() > 50);
        assertEquals("MEDIUM", result.getPerformancePrediction());

        // Verify save was called
        verify(predictiveAnalysisRepository, times(1)).save(result);
    }

    @Test
    @DisplayName("Debe devolver factores neutrales si no hay datos históricos específicos")
    void whenNoSpecificHistoricalData_thenReturnNeutralFactors() {
        // Arrange: No hay partidos contra "Opponent D" ni en posición "GK"
        when(matchStatisticsRepository.findByPlayerName(playerName)).thenReturn(historicalMatches);
        when(performanceCalculator.calculateMetrics(anyList())).thenReturn(testMetrics);

        String opponent = "Opponent D";
        String position = "GK";

        // Act
        PredictiveAnalysis result = predictionService.predictPerformance(playerName, opponent, true, position);

        // Assert
        assertEquals(1.0, result.getOpponentFactor());
        assertEquals(1.0, result.getPositionFactor());
    }

    @Test
    @DisplayName("Debe manejar correctamente una lista de partidos vacía")
    void whenHistoricalDataIsEmpty_thenReturnsBasePrediction() {
        // Arrange
        when(matchStatisticsRepository.findByPlayerName(anyString())).thenReturn(Collections.emptyList());
        // Simulamos métricas vacías
        when(performanceCalculator.calculateMetrics(anyList())).thenReturn(new PerformanceMetrics());

        // Act
        PredictiveAnalysis result = predictionService.predictPerformance("New Player", "Any Opponent", false, "FW");

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.getGoalProbability());
        assertEquals(0.0, result.getAssistProbability());
        assertEquals(0.0, result.getFullMatchProbability());
        assertEquals("LOW", result.getPerformancePrediction());

        // Verificamos explícitamente que 'save' fue llamado para persistir el análisis
        // base
        verify(predictiveAnalysisRepository, times(1)).save(any(PredictiveAnalysis.class));
    }
}