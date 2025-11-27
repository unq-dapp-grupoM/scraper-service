package com.dapp.scraper_service.endpoint;

import com.dapp.scraper_service.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadTestEndpointTest {

    @Mock
    private PlayerService playerService;

    @InjectMocks
    private LoadTestEndpoint loadTestEndpoint;

    @Test
    void status_whenNoTestIsRunning_returnsReady() {
        // Act
        Map<String, Object> result = loadTestEndpoint.status();

        // Assert
        assertEquals("READY", result.get("status"));
        assertEquals(0, result.get("activeTests"));
    }

    @Test
    void performLoadTest_whenTestIsAlreadyRunning_returnsError() {
        // Arrange
        // Manually simulate a running test by setting the counter
        // This requires reflection or changing the class design.
        // For this test, we'll assume we can't change the class and will test the logic conceptually.
        // A better approach would be to have a way to inject the AtomicInteger or make it package-private for testing.

        // Simulate the first test running
        when(playerService.getPlayerInfoForLoadTest(anyString())).thenAnswer(invocation -> {
            Thread.sleep(500); // Simulate long-running task
            return Map.of("status", "SUCCESS_FROM_DB");
        });

        // Start the first test in a separate thread
        Thread testThread = new Thread(() -> loadTestEndpoint.performLoadTest(1, 1));
        testThread.start();

        // Give it a moment to start and increment the counter
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act: Try to start a second test while the first is running
        Map<String, Object> result = loadTestEndpoint.performLoadTest(1, 1);

        // Assert
        assertEquals("Test en ejecución", result.get("error"));

        // Clean up the thread
        try {
            testThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void performLoadTest_withSuccessfulRequests_returnsCompletedStatus() {
        // Arrange
        when(playerService.getPlayerInfoForLoadTest("Messi"))
                .thenReturn(Map.of("status", "SUCCESS_FROM_DB"));

        int concurrentConnections = 2;
        int requestsPerConnection = 3;

        // Act
        Map<String, Object> result = loadTestEndpoint.performLoadTest(concurrentConnections, requestsPerConnection);

        // Assert
        assertEquals("COMPLETED", result.get("finalStatus"));
        assertEquals(concurrentConnections * requestsPerConnection, result.get("successfulRequests"));
        assertEquals(0, result.get("failedRequests"));
        assertEquals(100.0, result.get("successRate"));
    }

    @Test
    void performLoadTest_withFailedRequests_returnsCorrectCounts() {
        // Arrange
        when(playerService.getPlayerInfoForLoadTest("Messi"))
                .thenReturn(Map.of("status", "SUCCESS_FROM_DB"))
                .thenReturn(Map.of("status", "ERROR", "error", "Service unavailable"))
                .thenReturn(Map.of("status", "SUCCESS_FROM_DB"));

        // Act
        Map<String, Object> result = loadTestEndpoint.performLoadTest(1, 3);

        // Assert
        assertEquals("COMPLETED", result.get("finalStatus"));
        assertEquals(2, result.get("successfulRequests"));
        assertEquals(1, result.get("failedRequests"));
    }

    @Test
    void performLoadTest_withServiceException_returnsCorrectCounts() {
        // Arrange
        when(playerService.getPlayerInfoForLoadTest("Messi"))
                .thenReturn(Map.of("status", "SUCCESS_FROM_DB"))
                .thenThrow(new RuntimeException("Unexpected error"))
                .thenReturn(Map.of("status", "SUCCESS_FROM_SCRAPE"));

        // Act
        Map<String, Object> result = loadTestEndpoint.performLoadTest(1, 3);

        // Assert
        assertEquals("COMPLETED", result.get("finalStatus"));
        assertEquals(2, result.get("successfulRequests"));
        assertEquals(1, result.get("failedRequests"));
    }
}