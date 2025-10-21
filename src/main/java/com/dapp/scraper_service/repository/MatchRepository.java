package com.dapp.scraper_service.repository;

import com.dapp.scraper_service.model.Match;
import com.dapp.scraper_service.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    // Busca todos los partidos asociados a una entidad Team.
    List<Match> findByTeam(Team team);
    // Elimina todos los partidos asociados a una entidad Team.
    void deleteByTeam(Team team);
}