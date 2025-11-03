package com.dapp.scraper_service.repository;

import com.dapp.scraper_service.model.QueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {

    List<QueryHistory> findByPlayerNameAndQueryDateAndUserId(String playerName, LocalDate queryDate, Long userId);
}