package com.dapp.scraper_service.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class ScraperInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        // Información de la aplicación
        Map<String, Object> appInfo = new HashMap<>();
        appInfo.put("name", "Scraper Service API");
        appInfo.put("version", "1.0.0");
        appInfo.put("description", "Sistema de gestión deportiva - Entrega 3");
        appInfo.put("team", "Grupo M");
        appInfo.put("status", "Operational");
        appInfo.put("timestamp", Instant.now().toString());

        // Features técnicos
        Map<String, Object> features = new HashMap<>();
        features.put("dataSource", "Scraper Service Integration");
        features.put("monitoring", "Spring Boot Actuator");
        features.put("database", "H2 In-Memory");
        features.put("documentation", "Swagger/OpenAPI 3");
        features.put("ciCd", "GitHub Actions + Render");

        // Endpoints deportivos disponibles
        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("players", "/api/scrape/teams/{id}/players");
        endpoints.put("matches", "/api/scrape/teams/{id}/futureMatches");
        endpoints.put("performance", "/api/scrape/players/{id}/performance");
        endpoints.put("prediction", "/api/scrape/matches/prediction");

        builder.withDetail("application", appInfo)
                .withDetail("technicalFeatures", features)
                .withDetail("availableEndpoints", endpoints)
                .withDetail("monitoring", "Visit /monitoring/health for status");
    }
}