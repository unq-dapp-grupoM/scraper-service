package com.dapp.scraper_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dapp.scraper_service.model.MatchStatistics;

@Repository
public interface MatchStatisticsRepository extends JpaRepository<MatchStatistics, Long> {

    List<MatchStatistics> findByPlayerName(String playerName);

}
