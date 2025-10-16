package com.dapp.scraper_service.repository;

import com.dapp.scraper_service.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    // Busca equipos cuyo nombre contenga el término de búsqueda (ignorando
    // mayúsculas/minúsculas)
    List<Team> findByNameContainingIgnoreCase(String name);

    // Busca un equipo por nombre exacto
    Optional<Team> findByName(String name);
}