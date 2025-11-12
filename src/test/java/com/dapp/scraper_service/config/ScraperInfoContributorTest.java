package com.dapp.scraper_service.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScraperInfoContributorTest {

    private ScraperInfoContributor scraperInfoContributor;
    private Info.Builder builder;

    @BeforeEach
    void setUp() {
        scraperInfoContributor = new ScraperInfoContributor();
        builder = new Info.Builder();
    }

    @Test
    void contribute() {
        scraperInfoContributor.contribute(builder);
        Info info = builder.build();

        // Verificar la información de la aplicación
        Map<String, Object> appInfo = (Map<String, Object>) info.getDetails().get("application");
        assertNotNull(appInfo);
        assertEquals("Scraper Service API", appInfo.get("name"));
        assertEquals("1.0.0", appInfo.get("version"));
        assertEquals("Sistema de gestión deportiva - Entrega 3", appInfo.get("description"));
        assertEquals("Grupo M", appInfo.get("team"));
        assertEquals("Operational", appInfo.get("status"));
        assertNotNull(appInfo.get("timestamp"));
        try {
            Instant.parse((String) appInfo.get("timestamp"));
        } catch (Exception e) {
            fail("El timestamp no es un Instant válido.");
        }

        // Verificar las características técnicas
        Map<String, Object> features = (Map<String, Object>) info.getDetails().get("technicalFeatures");
        assertNotNull(features);
        assertEquals("Scraper Service Integration", features.get("dataSource"));
        assertEquals("Spring Boot Actuator", features.get("monitoring"));
        assertEquals("H2 In-Memory", features.get("database"));
        assertEquals("Swagger/OpenAPI 3", features.get("documentation"));
        assertEquals("GitHub Actions + Render", features.get("ciCd"));

        // Verificar los endpoints disponibles
        Map<String, Object> endpoints = (Map<String, Object>) info.getDetails().get("availableEndpoints");
        assertNotNull(endpoints);
        assertEquals("/api/scrape/teams/{id}/players", endpoints.get("players"));
        assertEquals("/api/scrape/teams/{id}/futureMatches", endpoints.get("matches"));
        assertEquals("/api/scrape/players/{id}/performance", endpoints.get("performance"));
        assertEquals("/api/scrape/matches/prediction", endpoints.get("prediction"));

        // Verificar la información de monitoreo
        assertEquals("Visit /monitoring/health for status", info.getDetails().get("monitoring"));
    }
}