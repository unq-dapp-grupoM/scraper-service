package com.dapp.scraper_service;

import com.dapp.scraper_service.model.MatchStatistics;
import com.dapp.scraper_service.model.PerformanceMetrics;
import com.dapp.scraper_service.repository.PerformanceMetricsRepository;
import com.dapp.scraper_service.service.PerformanceCalculatorService;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceCalculatorServiceTest {

    @Mock
    private PerformanceMetricsRepository metricsRepository;

    @InjectMocks
    private PerformanceCalculatorService calculatorService;

    private List<MatchStatistics> matches;

    @BeforeEach
    void setUp() {
        // Crear una lista de partidos de prueba
        matches = new ArrayList<>();
        matches.add(createMatch("Test Player", "2024-09-01", 90, 1, 0, 7.5, 3, 80.0, 2));
        matches.add(createMatch("Test Player", "2024-09-08", 90, 0, 1, 8.5, 1, 90.0, 1));
        matches.add(createMatch("Test Player", "2024-09-15", 90, 2, 1, 9.5, 5, 85.0, 3));
    }

    private MatchStatistics createMatch(String playerName, String date, int mins, int goals, int assists, double rating,
            int shots, double passAcc, int aerials) {
        MatchStatistics match = new MatchStatistics();
        match.setPlayerName(playerName);
        match.setMatchDate(LocalDate.parse(date));
        match.setMinutesPlayed(mins);
        match.setGoals(goals);
        match.setAssists(assists);
        match.setRating(rating);
        match.setShots(shots);
        match.setPassAccuracy(passAcc);
        match.setAerialDuels(aerials);
        return match;
    }

    @Test
    @DisplayName("Debe devolver métricas vacías si la lista de partidos está vacía")
    void whenMatchListIsEmpty_thenReturnEmptyMetrics() {
        // Act
        PerformanceMetrics result = calculatorService.calculateMetrics(Collections.emptyList());

        // Assert
        assertNotNull(result);
        assertNull(result.getPlayerName());
        assertEquals(6.0, result.getAverageRating());
    }

    @Test
    @DisplayName("Debe devolver métricas vacías pero con nombre si no se jugaron minutos")
    void whenNoMinutesPlayed_thenReturnEmptyMetricsWithPlayerName() {
        // Arrange
        List<MatchStatistics> zeroMinuteMatches = List.of(
                createMatch("Test Player", "2024-09-01", 0, 0, 0, 0.0, 0, 0.0, 0));

        // Act
        PerformanceMetrics result = calculatorService.calculateMetrics(zeroMinuteMatches);

        // Assert
        assertNotNull(result);
        assertEquals("Test Player", result.getPlayerName());
        assertEquals(6.0, result.getAverageRating());
        assertEquals(0.0, result.getGoalsPerMatch());
    }

    @Test
    @DisplayName("Debe calcular todas las métricas correctamente para una lista de partidos válida")
    void whenGivenValidMatches_thenCalculateAllMetricsCorrectly() {
        // Arrange: Configurar el mock solo para este test
        when(metricsRepository.save(any(PerformanceMetrics.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PerformanceMetrics result = calculatorService.calculateMetrics(matches);

        // Assert
        assertEquals("Test Player", result.getPlayerName());

        // Total Goals = 1+0+2 = 3. Total Matches = 3. GoalsPerMatch = 1.0
        assertEquals(1.0, result.getGoalsPerMatch());

        // Total Assists = 0+1+1 = 2. Total Matches = 3. AssistsPerMatch = 2/3
        assertEquals(2.0 / 3.0, result.getAssistsPerMatch(), 0.001);

        // GoalInvolvement = (3+2)/3 = 5/3
        assertEquals(5.0 / 3.0, result.getGoalInvolvement(), 0.001);

        // Total Shots = 3+1+5 = 9. Total Matches = 3. ShotsPerMatch = 3.0
        assertEquals(3.0, result.getShotsPerMatch());

        // Avg Pass Accuracy = (80+90+85)/3 = 85.0
        assertEquals(85.0, result.getPassAccuracy(), 0.001);

        // Avg Rating = (7.5+8.5+9.5)/3 = 8.5
        assertEquals(8.5, result.getAverageRating(), 0.001);

        // Rating Deviation = sqrt( ((7.5-8.5)^2 + (8.5-8.5)^2 + (9.5-8.5)^2) / 2 ) =
        // sqrt( (1+0+1)/2 ) = sqrt(1) = 1.0
        assertEquals(1.0, result.getRatingDeviation(), 0.001);

        // Goal Probability = 2 matches with goals / 3 total = 2/3
        assertEquals(2.0 / 3.0, result.getGoalProbability(), 0.001);

        // Assist Probability = 2 matches with assists / 3 total = 2/3
        assertEquals(2.0 / 3.0, result.getAssistProbability(), 0.001);

        // Offensive Impact = (1.0 * 0.4) + (0.666 * 0.3) + (8.5 * 0.2 / 10) + (3.0 *
        // 0.1 / 10)
        // = 0.4 + 0.2 + 0.17 + 0.03 = 0.8
        assertEquals(0.8, result.getOffensiveImpact(), 0.001);

        // Aerials Won = (2+1+3)/3 = 2.0
        assertEquals(2.0, result.getAerialDuelsWon(), 0.001);

        // Minutes Per Match = (90+90+90)/3 = 90.0
        assertEquals(90.0, result.getMinutesPerMatch());
    }

    @Test
    @DisplayName("Debe calcular la tendencia de rendimiento correctamente")
    void whenCalculatingPerformanceTrend_thenCompareRecentVsOlderMatches() {
        // Arrange: Configurar el mock solo para este test
        when(metricsRepository.save(any(PerformanceMetrics.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Arrange: Añadimos más partidos para tener un historial más largo
        matches.add(0, createMatch("Test Player", "2024-08-15", 90, 0, 0, 6.0, 1, 70.0, 1));
        matches.add(0, createMatch("Test Player", "2024-08-22", 90, 1, 0, 7.0, 2, 75.0, 2));
        // Ahora hay 5 partidos. n = min(5, 5/2) = 2
        // Recientes: 9.5, 8.5 -> Avg = 9.0
        // Antiguos: 6.0, 7.0 -> Avg = 6.5
        // Trend = 9.0 - 6.5 = 2.5

        // Act
        PerformanceMetrics result = calculatorService.calculateMetrics(matches);

        // Assert
        assertEquals(2.5, result.getPerformanceTrend(), 0.001);
    }

    @Test
    @DisplayName("Debe devolver una desviación de 1.0 si hay menos de 2 partidos")
    void whenLessThanTwoMatches_thenRatingDeviationIsOne() {
        // Arrange
        // Configurar el mock para que devuelva el objeto que se le pasa para guardar
        when(metricsRepository.save(any(PerformanceMetrics.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<MatchStatistics> singleMatch = List.of(matches.get(0));

        // Act
        PerformanceMetrics result = calculatorService.calculateMetrics(singleMatch);

        // Assert
        assertEquals(1.0, result.getRatingDeviation());
    }

    @Test
    @DisplayName("Debe llamar al método save del repositorio al final")
    void whenMetricsAreCalculated_thenRepositorySaveIsCalled() {
        // Arrange: Configurar el mock solo para este test
        when(metricsRepository.save(any(PerformanceMetrics.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Arrange
        ArgumentCaptor<PerformanceMetrics> metricsCaptor = ArgumentCaptor.forClass(PerformanceMetrics.class);

        // Act
        calculatorService.calculateMetrics(matches);

        // Assert
        verify(metricsRepository).save(metricsCaptor.capture());
        PerformanceMetrics savedMetrics = metricsCaptor.getValue();
        assertNotNull(savedMetrics);
        assertEquals("Test Player", savedMetrics.getPlayerName());
    }
}