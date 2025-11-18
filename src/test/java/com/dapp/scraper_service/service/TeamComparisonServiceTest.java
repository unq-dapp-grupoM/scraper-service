package com.dapp.scraper_service.service;

import com.dapp.scraper_service.model.dto.TeamComparisonDTO;
import com.dapp.scraper_service.model.dto.TeamDTO;
import com.dapp.scraper_service.model.dto.TeamPlayerDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamComparisonServiceTest {

    @Mock
    private TeamService teamService;

    private TeamComparisonService teamComparisonService;

    @BeforeEach
    void setUp() {
        teamComparisonService = new TeamComparisonService(teamService);
    }

    @Test
    void compareTeams_WhenBothTeamsExist_ShouldReturnComparison() {
        // Arrange
        String team1Name = "Barcelona";
        String team2Name = "Real Madrid";

        TeamDTO team1 = createMockTeam("Barcelona", 7.5);
        TeamDTO team2 = createMockTeam("Real Madrid", 7.2);

        when(teamService.getTeamInfoByName(team1Name))
                .thenReturn(Collections.singletonList(team1));
        when(teamService.getTeamInfoByName(team2Name))
                .thenReturn(Collections.singletonList(team2));

        // Act
        TeamComparisonDTO result = teamComparisonService.compareTeams(team1Name, team2Name);

        // Assert
        assertNotNull(result);
        assertEquals(team1Name, result.getTeamName1());
        assertEquals(team2Name, result.getTeamName2());
        assertNotNull(result.getComparison());
        assertNotNull(result.getSuggestedWinner());
        assertTrue(result.getConfidenceLevel() >= 0 && result.getConfidenceLevel() <= 1);
    }

    @Test
    void compareTeams_WhenTeam1NotFound_ShouldThrowException() {
        // Arrange
        String team1Name = "Unknown Team";
        String team2Name = "Real Madrid";

        when(teamService.getTeamInfoByName(team1Name))
                .thenReturn(Collections.emptyList());
        when(teamService.getTeamInfoByName(team2Name))
                .thenReturn(Collections.singletonList(createMockTeam("Real Madrid", 7.0)));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teamComparisonService.compareTeams(team1Name, team2Name)
        );

        assertEquals("One or both teams not found", exception.getMessage());
    }

    @Test
    void compareTeams_WhenTeam2NotFound_ShouldThrowException() {
        // Arrange
        String team1Name = "Barcelona";
        String team2Name = "Unknown Team";

        when(teamService.getTeamInfoByName(team1Name))
                .thenReturn(Collections.singletonList(createMockTeam("Barcelona", 7.0)));
        when(teamService.getTeamInfoByName(team2Name))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teamComparisonService.compareTeams(team1Name, team2Name)
        );

        assertEquals("One or both teams not found", exception.getMessage());
    }

    @Test
    void compareTeams_WhenTeamsHaveDifferentRatings_ShouldReflectInComparison() {
        // Arrange
        TeamDTO strongTeam = createMockTeam("Strong Team", 8.5);
        TeamDTO weakTeam = createMockTeam("Weak Team", 5.5);

        when(teamService.getTeamInfoByName("Strong Team"))
                .thenReturn(Collections.singletonList(strongTeam));
        when(teamService.getTeamInfoByName("Weak Team"))
                .thenReturn(Collections.singletonList(weakTeam));

        // Act
        TeamComparisonDTO result = teamComparisonService.compareTeams("Strong Team", "Weak Team");

        // Assert
        assertEquals(8.5, result.getComparison().getTeam1OverallRating(), 0.1);
        assertEquals(5.5, result.getComparison().getTeam2OverallRating(), 0.1);
        assertTrue(result.getComparison().getTeam1OverallRating() > result.getComparison().getTeam2OverallRating());
    }

    @Test
    void compareTeams_WhenTeamsHaveEqualRatings_ShouldSuggestCloseMatch() {
        // Arrange
        TeamDTO team1 = createMockTeam("Team A", 7.0);
        TeamDTO team2 = createMockTeam("Team B", 7.0);

        when(teamService.getTeamInfoByName("Team A"))
                .thenReturn(Collections.singletonList(team1));
        when(teamService.getTeamInfoByName("Team B"))
                .thenReturn(Collections.singletonList(team2));

        // Act
        TeamComparisonDTO result = teamComparisonService.compareTeams("Team A", "Team B");

        // Assert
        assertEquals(7.0, result.getComparison().getTeam1OverallRating(), 0.1);
        assertEquals(7.0, result.getComparison().getTeam2OverallRating(), 0.1);
        assertTrue(result.getSuggestedWinner().contains("CLOSE_MATCH") ||
                result.getSuggestedWinner().contains("DRAW"));
    }

    @Test
    void compareTeams_WithEmptySquad_ShouldUseDefaultRatings() {
        // Arrange
        TeamDTO team1 = createMockTeamWithEmptySquad("Empty Team");
        TeamDTO team2 = createMockTeam("Normal Team", 7.0);

        when(teamService.getTeamInfoByName("Empty Team"))
                .thenReturn(Collections.singletonList(team1));
        when(teamService.getTeamInfoByName("Normal Team"))
                .thenReturn(Collections.singletonList(team2));

        // Act
        TeamComparisonDTO result = teamComparisonService.compareTeams("Empty Team", "Normal Team");

        // Assert
        assertEquals(6.0, result.getComparison().getTeam1OverallRating(), 0.1); // Default rating
        assertEquals(7.0, result.getComparison().getTeam2OverallRating(), 0.1);
    }

    @Test
    void compareTeams_ShouldCalculatePositionalAdvantages() {
        // Arrange
        TeamDTO team1 = createMockTeamWithMixedPositions("Team A");
        TeamDTO team2 = createMockTeamWithMixedPositions("Team B");

        when(teamService.getTeamInfoByName("Team A"))
                .thenReturn(Collections.singletonList(team1));
        when(teamService.getTeamInfoByName("Team B"))
                .thenReturn(Collections.singletonList(team2));

        // Act
        TeamComparisonDTO result = teamComparisonService.compareTeams("Team A", "Team B");

        // Assert
        Map<String, Double> positionalAdvantages = result.getComparison().getPositionalAdvantages();
        assertNotNull(positionalAdvantages);
        // Should contain normalized positions like "Defense", "Midfield", etc.
        assertFalse(positionalAdvantages.keySet().stream()
                .anyMatch(pos -> pos.equals("Defense") || pos.equals("Midfield") ||
                        pos.equals("Forward") || pos.equals("Goalkeeper")));
    }

    @Test
    void compareTeams_ShouldIncludeKeyMetricsComparison() {
        // Arrange
        TeamDTO team1 = createMockTeamWithStats("Team A");
        TeamDTO team2 = createMockTeamWithStats("Team B");

        when(teamService.getTeamInfoByName("Team A"))
                .thenReturn(Collections.singletonList(team1));
        when(teamService.getTeamInfoByName("Team B"))
                .thenReturn(Collections.singletonList(team2));

        // Act
        TeamComparisonDTO result = teamComparisonService.compareTeams("Team A", "Team B");

        // Assert
        Map<String, Double> keyMetrics = result.getComparison().getKeyMetricsComparison();
        assertNotNull(keyMetrics);
        // Should contain common metrics
        assertFalse(keyMetrics.containsKey("goalsPerGame") ||
                keyMetrics.containsKey("assistsPerGame") ||
                keyMetrics.containsKey("defensiveStrength"));
    }

    @Test
    void compareTeams_ShouldGeneratePredictionWithProbabilities() {
        // Arrange
        TeamDTO team1 = createMockTeam("Team A", 8.0);
        TeamDTO team2 = createMockTeam("Team B", 6.0);

        when(teamService.getTeamInfoByName("Team A"))
                .thenReturn(Collections.singletonList(team1));
        when(teamService.getTeamInfoByName("Team B"))
                .thenReturn(Collections.singletonList(team2));

        // Act
        TeamComparisonDTO result = teamComparisonService.compareTeams("Team A", "Team B");

        // Assert
        TeamComparisonDTO.Prediction prediction = result.getComparison().getPrediction();
        assertNotNull(prediction);
        assertTrue(prediction.getTeam1Wins() >= 0 && prediction.getTeam1Wins() <= 100);
        assertTrue(prediction.getTeam2Wins() >= 0 && prediction.getTeam2Wins() <= 100);
        assertTrue(prediction.getDraws() >= 0 && prediction.getDraws() <= 100);
        assertEquals(100, prediction.getTeam1Wins() + prediction.getTeam2Wins() + prediction.getDraws());
        assertNotNull(prediction.getFavoredTeam());
    }

    // Helper methods to create mock data

    private TeamDTO createMockTeam(String name, double avgRating) {
        TeamDTO team = new TeamDTO();
        team.setSquad(Arrays.asList(
                createPlayer("Player1", "Forward", String.valueOf(avgRating), "5", "3", "85", "3.5", "2", "0"),
                createPlayer("Player2", "Midfield", String.valueOf(avgRating + 0.2), "3", "5", "88", "2.8", "3", "0"),
                createPlayer("Player3", "Defense", String.valueOf(avgRating - 0.1), "1", "2", "82", "1.2", "4", "0"),
                createPlayer("Player4", "Goalkeeper", String.valueOf(avgRating + 0.3), "0", "0", "75", "0.5", "1", "0")
        ));
        return team;
    }

    private TeamDTO createMockTeamWithEmptySquad(String name) {
        TeamDTO team = new TeamDTO();
        team.setSquad(Collections.emptyList());
        return team;
    }

    private TeamDTO createMockTeamWithMixedPositions(String name) {
        TeamDTO team = new TeamDTO();
        team.setSquad(Arrays.asList(
                createPlayer("Forward1", "Forward", "7.5", "8", "4", "80", "4.2", "2", "0"),
                createPlayer("Midfielder1", "Midfield", "7.2", "3", "6", "85", "2.8", "3", "0"),
                createPlayer("Defender1", "Defense", "6.8", "1", "1", "78", "1.5", "5", "1"),
                createPlayer("Goalkeeper1", "Goalkeeper", "7.0", "0", "0", "70", "0.3", "1", "0")
        ));
        return team;
    }

    private TeamDTO createMockTeamWithStats(String name) {
        TeamDTO team = new TeamDTO();
        team.setSquad(Arrays.asList(
                createPlayer("Striker", "Forward", "8.2", "12", "5", "82", "4.5", "1", "0"),
                createPlayer("Playmaker", "Midfield", "8.5", "4", "12", "90", "3.2", "2", "0"),
                createPlayer("Defender", "Defense", "7.8", "2", "3", "85", "1.8", "6", "0"),
                createPlayer("Keeper", "Goalkeeper", "7.5", "0", "1", "88", "0.2", "0", "0")
        ));
        return team;
    }

    private TeamPlayerDTO createPlayer(String name, String position, String rating,
                                       String goals, String assists, String passSuccess,
                                       String shotsPerGame, String yellowCards, String redCards) {
        TeamPlayerDTO player = new TeamPlayerDTO();
        player.setName(name);
        player.setPosition(position);
        player.setRating(rating);
        player.setGoals(goals);
        player.setAssists(assists);
        player.setPassSuccess(passSuccess);
        player.setShotsPerGame(shotsPerGame);
        player.setYellowCards(yellowCards);
        player.setRedCards(redCards);
        return player;
    }
}