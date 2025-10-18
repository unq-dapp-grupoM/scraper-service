package com.dapp.scraper_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dapp.scraper_service.model.PerformanceMetrics;

public interface PerformanceMetricsRepository extends JpaRepository<PerformanceMetrics, Long> {
    PerformanceMetrics findByPlayerName(String playerName);
}
