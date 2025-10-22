package com.dapp.scraper_service.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
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
}