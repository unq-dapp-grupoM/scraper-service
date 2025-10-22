package com.dapp.scraper_service.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
public class DatabaseDebugController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/tables")
    public Map<String, Object> listTables() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Listar todas las tablas en la base de datos
            List<String> tables = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                    String.class);
            result.put("tables", tables);
            result.put("totalTables", tables.size());
            result.put("status", "SUCCESS");

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }

        return result;
    }

    @PostMapping("/force-create-tables")
    public Map<String, Object> forceCreateTables() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Forzar creación usando JPA
            jdbcTemplate.execute("SELECT 1"); // Activa la conexión

            // 2. Crear tablas manualmente si es necesario
            String[] createTables = {
                    "CREATE TABLE IF NOT EXISTS players (" +
                            "id BIGSERIAL PRIMARY KEY, " +
                            "name VARCHAR(255) NOT NULL UNIQUE, " +
                            "current_team VARCHAR(255), " +
                            "shirt_number VARCHAR(10), " +
                            "age VARCHAR(50), " +
                            "height VARCHAR(50), " +
                            "nationality VARCHAR(100), " +
                            "positions VARCHAR(255))",

                    "CREATE TABLE IF NOT EXISTS player_match_stats (" +
                            "id BIGSERIAL PRIMARY KEY, " +
                            "opponent VARCHAR(255), " +
                            "score VARCHAR(100), " +
                            "date VARCHAR(50), " +
                            "position VARCHAR(100), " +
                            "mins_played VARCHAR(50), " +
                            "goals VARCHAR(50), " +
                            "assists VARCHAR(50), " +
                            "yellow_cards VARCHAR(50), " +
                            "red_cards VARCHAR(50), " +
                            "shots VARCHAR(50), " +
                            "pass_success VARCHAR(50), " +
                            "aerials_won VARCHAR(50), " +
                            "rating VARCHAR(50), " +
                            "player_id BIGINT, " +
                            "FOREIGN KEY (player_id) REFERENCES players(id))"
            };

            for (String sql : createTables) {
                jdbcTemplate.execute(sql);
            }

            result.put("status", "SUCCESS");
            result.put("message", "Tablas creadas exitosamente");

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }

        return result;
    }
}