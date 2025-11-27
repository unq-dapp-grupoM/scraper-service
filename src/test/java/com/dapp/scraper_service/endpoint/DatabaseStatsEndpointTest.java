package com.dapp.scraper_service.endpoint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseStatsEndpointTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private DatabaseStatsEndpoint databaseStatsEndpoint;

    private void setupSuccessfulConnection() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void testGetDatabaseStats_HsqlDbSuccess() throws SQLException {
        // Arrange
        setupSuccessfulConnection();
        when(metaData.getDatabaseProductName()).thenReturn("HSQL Database Engine");
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("count")).thenReturn(10L);
        when(resultSet.getInt(1)).thenReturn(5);

        // Act
        Map<String, Object> stats = databaseStatsEndpoint.getDatabaseStats();

        // Assert
        assertNotNull(stats);
        assertEquals("OK", stats.get("databaseConnection"));
        assertEquals("HSQL Database Engine", stats.get("databaseProduct"));
        assertEquals(10L, stats.get("totalPlayers"));
        assertEquals(10L, stats.get("totalTeams"));
        assertEquals(5, stats.get("activeConnections"));
        assertEquals("In-Memory Database", stats.get("databaseSize"));
    }

    @Test
    void testGetDatabaseStats_PostgresDbSuccess() throws SQLException {
        // Arrange
        setupSuccessfulConnection();
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("count")).thenReturn(20L);
        when(resultSet.getInt("count")).thenReturn(8);

        // Act
        Map<String, Object> stats = databaseStatsEndpoint.getDatabaseStats();

        // Assert
        assertNotNull(stats);
        assertEquals("OK", stats.get("databaseConnection"));
        assertEquals("PostgreSQL", stats.get("databaseProduct"));
        assertEquals(20L, stats.get("totalPlayers"));
        assertEquals(20L, stats.get("totalTeams"));
        assertEquals(8, stats.get("activeConnections"));
        assertEquals("PostgreSQL", stats.get("databaseSize"));
    }

    @Test
    void testGetDatabaseStats_SqlConnectionError() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // Act
        Map<String, Object> stats = databaseStatsEndpoint.getDatabaseStats();

        // Assert
        assertNotNull(stats);
        assertEquals("ERROR", stats.get("databaseConnection"));
        assertEquals("Connection failed", stats.get("error"));
    }

    @Test
    void testGetDatabaseStats_TableCountError() throws SQLException {
        // Arrange
        setupSuccessfulConnection();
        when(metaData.getDatabaseProductName()).thenReturn("HSQL Database Engine");
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("count")).thenReturn(50L); // For successful queries

        // Override the stub for the specific failing query
        when(connection.prepareStatement("SELECT COUNT(*) as count FROM PLAYERS")).thenThrow(new SQLException("Table not found"));

        // Act
        Map<String, Object> stats = databaseStatsEndpoint.getDatabaseStats();

        // Assert
        assertNotNull(stats);
        assertEquals("OK", stats.get("databaseConnection"));
        assertEquals(-1L, stats.get("totalPlayers"));
        assertEquals(50L, stats.get("totalTeams")); // Verify other tables were counted
    }
}