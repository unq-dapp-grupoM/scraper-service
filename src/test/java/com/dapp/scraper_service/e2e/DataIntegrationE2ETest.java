package com.dapp.scraper_service.e2e;

import com.dapp.scraper_service.ScraperServiceApplication;
import com.dapp.scraper_service.model.MatchStatistics;
import com.dapp.scraper_service.model.Player;
import com.dapp.scraper_service.model.PlayerMatchStats;
import com.dapp.scraper_service.repository.PlayerRepository;
import com.dapp.scraper_service.service.DataIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ScraperServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:datatest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.web-application-type=none",
        "spring.cache.type=none"
})
@Transactional
class DataIntegrationE2ETest {

    @Autowired
    private DataIntegrationService dataIntegrationService;

    @Autowired
    private PlayerRepository playerRepository;

    @BeforeEach
    void setUp() {
        playerRepository.deleteAll();
    }

    @Test
    void testDataIntegrationWithCompleteData() {
        Player player = new Player();
        player.setName("Integration Test Player");

        PlayerMatchStats stats = new PlayerMatchStats();
        stats.setOpponent("Test Opponent");
        stats.setDate("15/09/2024");
        stats.setScore("2-1");
        stats.setPosition("Delantero");
        stats.setMinsPlayed("90");
        stats.setGoals("1");
        stats.setAssists("0");
        stats.setYellowCards("0");
        stats.setRedCards("0");
        stats.setShots("3");
        stats.setPassSuccess("85.5");
        stats.setAerialsWon("2");
        stats.setRating("7.5");
        stats.setPlayer(player);

        player.setMatchStats(List.of(stats));
        playerRepository.save(player);

        List<MatchStatistics> result = dataIntegrationService.convertToMatchStatistics("Integration Test Player");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Integration Test Player", result.get(0).getPlayerName());
        assertEquals("FW", result.get(0).getPosition());
        assertEquals(1, result.get(0).getGoals());
        assertEquals(7.5, result.get(0).getRating());
    }

    @Test
    void testDataIntegrationWithEmptyData() {
        Player player = new Player();
        player.setName("Minimal Data Player");

        PlayerMatchStats stats = new PlayerMatchStats();
        stats.setOpponent("Minimal Opponent");
        stats.setDate("10/09/2024");
        stats.setPosition("Mediocampista");
        stats.setMinsPlayed("60");
        stats.setPlayer(player);

        player.setMatchStats(List.of(stats));
        playerRepository.save(player);

        List<MatchStatistics> result = dataIntegrationService.convertToMatchStatistics("Minimal Data Player");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Minimal Data Player", result.get(0).getPlayerName());
        assertEquals("MF", result.get(0).getPosition());
    }

    @Test
    void testDataIntegrationWithMultiplePlayers() {

        Player player1 = new Player();
        player1.setName("Player One");

        PlayerMatchStats stats1 = new PlayerMatchStats();
        stats1.setOpponent("Team A");
        stats1.setDate("15/09/2024");
        stats1.setPosition("Defensa");
        stats1.setMinsPlayed("90");
        stats1.setGoals("0");
        stats1.setRating("7.0");
        stats1.setPlayer(player1);

        Player player2 = new Player();
        player2.setName("Player Two");

        PlayerMatchStats stats2 = new PlayerMatchStats();
        stats2.setOpponent("Team B");
        stats2.setDate("16/09/2024");
        stats2.setPosition("Delantero");
        stats2.setMinsPlayed("85");
        stats2.setGoals("2");
        stats2.setRating("8.5");
        stats2.setPlayer(player2);

        player1.setMatchStats(List.of(stats1));
        player2.setMatchStats(List.of(stats2));

        playerRepository.saveAll(List.of(player1, player2));

        List<MatchStatistics> result = dataIntegrationService.convertToMatchStatistics("Player One");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Player One", result.get(0).getPlayerName());
        assertEquals("DF", result.get(0).getPosition());
    }
}