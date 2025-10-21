package com.dapp.scraper_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private Environment environment;

    @Value("${spring.datasource.url:unknown}")
    private String databaseUrl;

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("database", getDatabaseInfo());
        status.put("activeProfiles", environment.getActiveProfiles());
        status.put("defaultProfiles", environment.getDefaultProfiles());

        return status;
    }

    private String getDatabaseInfo() {
        String url = databaseUrl.toLowerCase();
        if (url.contains("hsqldb")) {
            return "HSQLDB (Development)";
        } else if (url.contains("postgres")) {
            return "PostgreSQL (Production)";
        } else {
            return "Unknown: " + databaseUrl;
        }
    }
}