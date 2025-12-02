package com.dapp.scraper_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final Environment environment;
    private final DataSource dataSource;

    @Autowired(required = false) // required=false para evitar errores si no hay DataSource
    public HealthController(Environment environment, DataSource dataSource) {
        this.environment = environment;
        this.dataSource = dataSource;
    }
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("activeProfiles", environment.getActiveProfiles());

        // Database information
        String datasourceUrl = environment.getProperty("spring.datasource.url", "unknown");
        status.put("databaseUrl", datasourceUrl);
        status.put("databaseType", getDatabaseType(datasourceUrl));
        status.put("dataSourcePresent", dataSource != null);

        if (dataSource != null) {
            try {
                status.put("dataSourceClass", dataSource.getClass().getSimpleName());
                // Basic connection test
                dataSource.getConnection().close();
                status.put("databaseConnection", "SUCCESS");
            } catch (Exception e) {
                status.put("databaseConnection", "FAILED: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(status);
    }

    private String getDatabaseType(String url) {
        if (url == null)
            return "Unknown";
        if (url.contains("postgresql"))
            return "PostgreSQL";
        if (url.contains("hsqldb"))
            return "HSQLDB";
        return "Unknown: " + url;
    }
}