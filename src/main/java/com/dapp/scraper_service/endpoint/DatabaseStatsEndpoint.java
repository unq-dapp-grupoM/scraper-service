package com.dapp.scraper_service.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Endpoint(id = "database-stats")
public class DatabaseStatsEndpoint {

    @Autowired
    private DataSource dataSource;

    @ReadOperation
    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("timestamp", LocalDateTime.now());

        try (Connection conn = dataSource.getConnection()) {
            String dbProduct = conn.getMetaData().getDatabaseProductName();
            boolean isHsql = dbProduct.toLowerCase().contains("hsql");

            stats.put("databaseConnection", "OK");
            stats.put("databaseProduct", dbProduct);
            stats.put("databaseVersion", conn.getMetaData().getDatabaseProductVersion());

            // Contar tablas con nombres correctos para cada BD
            stats.put("totalPlayers", getTableCount(conn, isHsql ? "PLAYERS" : "players"));
            stats.put("totalTeams", getTableCount(conn, isHsql ? "TEAMS" : "teams"));
            stats.put("totalPlayerMatchStats", getTableCount(conn, isHsql ? "PLAYER_MATCH_STATS" : "player_match_stats"));
            stats.put("totalTeamPlayers", getTableCount(conn, isHsql ? "TEAM_PLAYERS" : "team_players"));
            stats.put("totalMatchStatistics", getTableCount(conn, isHsql ? "MATCH_STATISTICS" : "match_statistics"));
            stats.put("totalPerformanceMetrics", getTableCount(conn, isHsql ? "PERFORMANCE_METRICS" : "performance_metrics"));
            stats.put("totalPredictiveAnalysis", getTableCount(conn, isHsql ? "PREDICTIVE_ANALYSIS" : "predictive_analysis"));
            stats.put("totalQueryHistory", getTableCount(conn, isHsql ? "QUERY_HISTORY" : "query_history"));

            // Para _user es especial - en HSQLDB necesita comillas si se creó en minúsculas
            if (isHsql) {
                // Intentar primero con _USER (sin comillas)
                long userCount = getTableCount(conn, "_USER");
                if (userCount == -1) {
                    // Si falla, intentar con comillas
                    userCount = getTableCountWithQuotes(conn, "_user");
                }
                stats.put("totalUsers", userCount);
            } else {
                stats.put("totalUsers", getTableCount(conn, "_user"));
            }

            // Consultas recientes
            stats.put("recentQueries24h", getRecentQueriesCount(conn, isHsql));

            // Conexiones activas
            stats.put("activeConnections", getActiveConnections(conn, isHsql));

            // Tamaño de BD
            stats.put("databaseSize", isHsql ? "In-Memory Database" : "PostgreSQL");

        } catch (SQLException e) {
            stats.put("databaseConnection", "ERROR");
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    private long getTableCount(Connection conn, String tableName) {
        String sql = "SELECT COUNT(*) as count FROM " + tableName;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong("count") : 0;
        } catch (SQLException e) {
            if (!"_USER".equals(tableName)) {
                System.err.println("Error contando tabla " + tableName + ": " + e.getMessage());
            }
            return -1;
        }
    }

    private long getTableCountWithQuotes(Connection conn, String tableName) {
        String sql = "SELECT COUNT(*) as count FROM \"" + tableName + "\"";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong("count") : 0;
        } catch (SQLException e) {
            System.err.println("Error contando tabla con comillas " + tableName + ": " + e.getMessage());
            return -1;
        }
    }

    private long getRecentQueriesCount(Connection conn, boolean isHsql) {
        String sql;
        if (isHsql) {
            // Para HSQLDB, verificar si la tabla tiene la columna CREATED_AT
            sql = "SELECT COUNT(*) as count FROM QUERY_HISTORY";
        } else {
            sql = "SELECT COUNT(*) as count FROM query_history WHERE created_at >= NOW() - INTERVAL '24 hours'";
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong("count") : 0;
        } catch (SQLException e) {
            System.err.println("Error contando queries recientes: " + e.getMessage());
            return -1;
        }
    }

    private int getActiveConnections(Connection conn, boolean isHsql) {
        try {
            if (isHsql) {
                String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SYSTEM_SESSIONS";
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } else {
                String sql = "SELECT count(*) as count FROM pg_stat_activity WHERE state = 'active'";
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt("count") : 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error contando conexiones: " + e.getMessage());
            return -1;
        }
    }
}