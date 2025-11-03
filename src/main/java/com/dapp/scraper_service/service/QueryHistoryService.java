package com.dapp.scraper_service.service;

import com.dapp.scraper_service.model.QueryHistory;
import com.dapp.scraper_service.model.QueryType;
import com.dapp.scraper_service.model.User;
import com.dapp.scraper_service.repository.QueryHistoryRepository;
import com.dapp.scraper_service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class QueryHistoryService {

    private final QueryHistoryRepository queryHistoryRepository;
    private final UserRepository userRepository;

    public QueryHistoryService(QueryHistoryRepository queryHistoryRepository, UserRepository userRepository) {
        this.queryHistoryRepository = queryHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordQuery(String userEmail, String playerName, QueryType queryType) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + userEmail));

        QueryHistory historyEntry = new QueryHistory(
                user.getId().longValue(),
                userEmail,
                playerName,
                queryType,
                LocalDate.now(),
                LocalTime.now()
        );
        queryHistoryRepository.save(historyEntry);
    }

    public List<QueryHistory> getHistory(String playerName, LocalDate date, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + userEmail));

        Long userId = user.getId().longValue();
        return queryHistoryRepository.findByPlayerNameAndQueryDateAndUserId(playerName, date, userId);
    }
}