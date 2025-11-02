package com.dapp.scraper_service.service;

import com.dapp.scraper_service.model.QueryHistory;
import com.dapp.scraper_service.model.QueryType;
import com.dapp.scraper_service.repository.QueryHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class QueryHistoryService {

    private final QueryHistoryRepository queryHistoryRepository;

    public QueryHistoryService(QueryHistoryRepository queryHistoryRepository) {
        this.queryHistoryRepository = queryHistoryRepository;
    }

    @Transactional
    public void recordQuery(Long userId, String playerName, QueryType queryType) {
        QueryHistory historyEntry = new QueryHistory(
                userId,
                playerName,
                queryType,
                LocalDate.now(),
                LocalTime.now()
        );
        queryHistoryRepository.save(historyEntry);
    }

    public List<QueryHistory> getHistory(String playerName, LocalDate date, Long userId) {
        return queryHistoryRepository.findByPlayerNameAndQueryDateAndUserId(playerName, date, userId);
    }
}