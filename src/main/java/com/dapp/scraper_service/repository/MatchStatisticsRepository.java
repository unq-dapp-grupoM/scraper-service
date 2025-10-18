package com.dapp.scraper_service.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dapp.scraper_service.model.MatchStatistics;

@Repository
public interface MatchStatisticsRepository extends JpaRepository<MatchStatistics, Long> {

    List<MatchStatistics> findByPlayerName(String playerName);

    /*
     * @Query("SELECT m FROM MatchStatistics m WHERE m.playerName = :playerName AND m.season = :season"
     * )
     * List<MatchStatistics> findByPlayerNameAndSeason(@Param("playerName") String
     * playerName,
     * 
     * @Param("season") String season);
     * 
     * @Query("SELECT m FROM MatchStatistics m WHERE m.playerName = :playerName AND m.opponent = :opponent"
     * )
     * List<MatchStatistics> findByPlayerNameAndOpponent(@Param("playerName") String
     * playerName,
     * 
     * @Param("opponent") String opponent);
     * 
     * @Query("SELECT m FROM MatchStatistics m WHERE m.playerName = :playerName AND m.position = :position"
     * )
     * List<MatchStatistics> findByPlayerNameAndPosition(@Param("playerName") String
     * playerName,
     * 
     * @Param("position") String position);
     * 
     * @Query("SELECT COUNT(m) FROM MatchStatistics m WHERE m.playerName = :playerName AND m.minutesPlayed >= 85"
     * )
     * Long countFullMatchesPlayed(@Param("playerName") String playerName);
     */
}
