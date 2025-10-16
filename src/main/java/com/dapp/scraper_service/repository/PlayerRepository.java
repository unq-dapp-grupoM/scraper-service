package com.dapp.scraper_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dapp.scraper_service.model.Player;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    // Busca jugadores cuyo nombre contenga el término de búsqueda (ignorando
    // mayúsculas/minúsculas)
    // Esto se traduce a una consulta SQL: WHERE lower(name) LIKE lower('%' || ? ||
    // '%')
    List<Player> findByNameContainingIgnoreCase(String name);

    // Busca un jugador por nombre exacto
    Optional<Player> findByName(String name);
}