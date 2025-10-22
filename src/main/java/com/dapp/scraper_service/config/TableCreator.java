package com.dapp.scraper_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class TableCreator {

    private static final Logger log = LoggerFactory.getLogger(TableCreator.class);

    @EventListener(ApplicationReadyEvent.class)
    public void forceTableCreation() {
        log.info("🚀 EJECUTANDO CREACIÓN FORZADA DE TABLAS...");
        // La simple existencia de este bean + ddl-auto=create forzará la creación
    }
}
