package com.dapp.scraper_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "query_history")
@Data
@NoArgsConstructor
public class QueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // En una app real, sería @ManyToOne User user;

    @Column(nullable = false)
    private String playerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueryType queryType;

    @Column(nullable = false)
    private LocalDate queryDate;

    @Column(nullable = false)
    private LocalTime queryTime;

    public QueryHistory(Long userId, String playerName, QueryType queryType, LocalDate queryDate, LocalTime queryTime) {
        this.userId = userId;
        this.playerName = playerName;
        this.queryType = queryType;
        this.queryDate = queryDate;
        this.queryTime = queryTime;
    }
}