package com.dapp.scraper_service.e2e;

import com.dapp.scraper_service.ScraperServiceApplication;
import com.dapp.scraper_service.model.Player;
import com.dapp.scraper_service.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@SpringBootTest(classes = ScraperServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:controllertest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.web-application-type=none",
        "spring.cache.type=none",
        "scraper.api.key=test-key-123"
})
class FunctionalControllerE2ETest {

    @Autowired
    private PlayerRepository playerRepository;

    @BeforeEach
    void setUp() {
        playerRepository.deleteAll();
    }

    @Test
    void testDatabaseOperations() {
        Player player = new Player();
        player.setName("Controller Test Player");
        player.setCurrentTeam("Test FC");

        Player saved = playerRepository.save(player);
        assertNotNull(saved.getId());

        Player found = playerRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Controller Test Player", found.getName());
    }

    @Test
    void testRepositoryOperations() {
        Player player1 = new Player();
        player1.setName("Player One");
        playerRepository.save(player1);

        Player player2 = new Player();
        player2.setName("Player Two");
        playerRepository.save(player2);

        List<Player> players = playerRepository.findByNameContainingIgnoreCase("Player");
        assertEquals(2, players.size());
    }
}