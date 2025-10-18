package com.dapp.scraper_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dapp.scraper_service.model.PredictiveAnalysis;

public interface PredictiveAnalysisRepository extends JpaRepository<PredictiveAnalysis, Long> {
    PredictiveAnalysis findByPlayerName(String playerName);
}
