package com.dapp.scraper_service.controller;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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

    @PostMapping("/create-all-tables")
    public Map<String, Object> createAllTables() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Leer el archivo schema.sql desde resources
            Resource resource = new ClassPathResource("schema.sql");
            String sqlScript = new String(Files.readAllBytes(Paths.get(resource.getURI())));

            // Dividir el script en sentencias individuales
            String[] sqlStatements = sqlScript.split(";");

            List<String> executedTables = new ArrayList<>();
            for (String sql : sqlStatements) {
                if (sql.trim().isEmpty())
                    continue;

                try {
                    jdbcTemplate.execute(sql.trim());

                    // Extraer nombre de tabla si es CREATE TABLE
                    if (sql.toUpperCase().contains("CREATE TABLE")) {
                        String tableName = extractTableName(sql);
                        if (tableName != null) {
                            executedTables.add(tableName);
                        }
                    }
                } catch (Exception e) {
                    // Ignorar errores de "tabla ya existe"
                    if (!e.getMessage().contains("already exists")) {
                        result.put("warning", "Some statements had issues: " + e.getMessage());
                    }
                }
            }

            result.put("status", "SUCCESS");
            result.put("executedTables", executedTables);
            result.put("totalStatements", sqlStatements.length);
            result.put("message", "Schema.sql ejecutado completamente");

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }

        return result;
    }

    private String extractTableName(String sql) {
        // Extraer nombre de tabla del CREATE TABLE
        Pattern pattern = Pattern.compile("CREATE TABLE (?:IF NOT EXISTS )?([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher matcher = pattern.matcher(sql.toUpperCase());
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }
        return null;
    }
}