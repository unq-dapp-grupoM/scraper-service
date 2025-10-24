package com.dapp.scraper_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

@Configuration
public class DatabaseConfig {

    @Autowired
    private Environment environment;

    @Bean
    @Profile("dev")
    public String devDatabaseInfo() {
        return "HSQLDB (Development) - Embedded database for local development";
    }

    @Bean
    @Profile("test")
    public String testDatabaseInfo() {
        return "HSQLDB (Testing) - In-memory database for tests";
    }

    @Bean
    @Profile("prod")
    public String prodDatabaseInfo() {
        return "PostgreSQL (Production) - Managed database on Render";
    }

    @Bean
    public String activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }
        return "Active profiles: " + Arrays.toString(profiles);
    }
}