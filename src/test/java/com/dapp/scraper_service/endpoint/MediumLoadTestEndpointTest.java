package com.dapp.scraper_service.endpoint;

import com.dapp.scraper_service.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediumLoadTestEndpointTest {

    @Mock
    private PlayerService playerService;

    @InjectMocks
    private MediumLoadTestEndpoint mediumLoadTestEndpoint;

    @Test
    void mediumTest_withSuccessfulRequests_returnsCompletedStatus() {
        // Arrange
        when(playerService.getPlayerInfoForLoadTest("Ronaldo"))
                .thenReturn(Map.of("status", "SUCCESS_FROM_DB"));

        // Act
        Map<String, Object> result = mediumLoadTestEndpoint.mediumTest();

        // Assert
        assertEquals("COMPLETED", result.get("finalStatus"));
        assertEquals(50, result.get("successfulRequests")); // 5 connections * 10 requests
        assertEquals(0, result.get("failedRequests"));
        assertEquals(100.0, result.get("successRate"));
        assertEquals("medium", result.get("testType"));
    }

    @Test
    void mediumTest_withSomeFailedRequests_returnsCorrectCounts() {
        // Arrange
        when(playerService.getPlayerInfoForLoadTest("Ronaldo"))
                .thenReturn(Map.of("status", "SUCCESS_FROM_DB")) // Should be called many times
                .thenReturn(Map.of("status", "ERROR", "error", "Service unavailable")) // Fail once
                .thenReturn(Map.of("status", "SUCCESS_FROM_SCRAPE")); // Succeed again

        // Act
        Map<String, Object> result = mediumLoadTestEndpoint.mediumTest();

        // Assert
        assertEquals("COMPLETED", result.get("finalStatus"));
        assertEquals(49, result.get("successfulRequests"));
        assertEquals(1, result.get("failedRequests"));
    }

    @Test
    void mediumTest_withServiceException_returnsCorrectCounts() {
        // Arrange
        when(playerService.getPlayerInfoForLoadTest("Ronaldo"))
                .thenReturn(Map.of("status", "SUCCESS_FROM_DB")) // Succeed
                .thenThrow(new RuntimeException("Unexpected database error")) // Fail
                .thenReturn(Map.of("status", "SUCCESS_FROM_SCRAPE")); // Succeed again

        // Act
        Map<String, Object> result = mediumLoadTestEndpoint.mediumTest();

        // Assert
        assertEquals("COMPLETED", result.get("finalStatus"));
        assertEquals(49, result.get("successfulRequests"));
        assertEquals(1, result.get("failedRequests"));
    }

    @Test
    void mediumTest_whenTestIsAlreadyRunning_returnsError() {
        // Arrange
        // Simulate a long-running task
        when(playerService.getPlayerInfoForLoadTest(anyString())).thenAnswer(invocation -> {
            Thread.sleep(500); // Simulate a delay
            return Map.of("status", "SUCCESS_FROM_DB");
        });

        // Start the first test in a background thread
        Thread testThread = new Thread(() -> mediumLoadTestEndpoint.mediumTest());
        testThread.start();

        // Give it a moment to start and increment the activeTests counter
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act: Try to start a second test while the first is presumably running
        Map<String, Object> result = mediumLoadTestEndpoint.mediumTest();

        // Assert
        assertEquals("Test en ejecución", result.get("error"));

        // Clean up: wait for the background thread to finish
        try {
            testThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}