package com.dapp.scraper_service.endpoint;

import com.dapp.scraper_service.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.stereotype.Component;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Endpoint(id = "load-test")
public class LoadTestEndpoint {

    @Autowired
    private PlayerService playerService;

    private final AtomicInteger activeTests = new AtomicInteger(0);

    // SOLO UN WriteOperation aquí
    @WriteOperation
    public Map<String, Object> performLoadTest(
            @Nullable Integer concurrentConnections,
            @Nullable Integer requestsPerConnection) {

        int connections = concurrentConnections != null ? concurrentConnections : 2;
        int requests = requestsPerConnection != null ? requestsPerConnection : 3;

        System.out.println("CUSTOM LOAD TEST - Ejecutando prueba personalizada");
        return executeLoadTest(connections, requests, "custom");
    }

    @ReadOperation
    public Map<String, Object> status() {
        return Map.of(
                "activeTests", activeTests.get(),
                "timestamp", LocalDateTime.now(),
                "status", activeTests.get() > 0 ? "RUNNING" : "READY",
                "availableEndpoints", new String[]{
                        "POST /monitoring/load-test-quick",
                        "POST /monitoring/load-test-medium",
                        "POST /monitoring/load-test",
                        "GET /monitoring/load-test"
                }
        );
    }

    private Map<String, Object> executeLoadTest(int concurrentConnections, int requestsPerConnection, String testType) {
        if (activeTests.get() > 0) {
            return Map.of("error", "Test en ejecución", "activeTests", activeTests.get());
        }

        activeTests.incrementAndGet();
        Map<String, Object> result = new HashMap<>();
        LocalDateTime startTime = LocalDateTime.now();

        try {
            AtomicInteger successfulRequests = new AtomicInteger(0);
            AtomicInteger failedRequests = new AtomicInteger(0);

            CompletableFuture<?>[] futures = new CompletableFuture[concurrentConnections];

            for (int i = 0; i < concurrentConnections; i++) {
                final int connectionId = i;
                futures[i] = CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < requestsPerConnection; j++) {
                        try {
                            Map<String, Object> requestResult = playerService.getPlayerInfoForLoadTest("Messi");

                            if ("SUCCESS_FROM_DB".equals(requestResult.get("status")) ||
                                    "SUCCESS_FROM_SCRAPE".equals(requestResult.get("status"))) {
                                successfulRequests.incrementAndGet();
                                System.out.println("QuickTest - Conexión " + connectionId + " - Request " + j + " exitosa");
                            } else {
                                failedRequests.incrementAndGet();
                                System.err.println("QuickTest - Conexión " + connectionId + " - Request " + j + " falló: " + requestResult.get("error"));
                            }

                            Thread.sleep(100);

                        } catch (Exception e) {
                            failedRequests.incrementAndGet();
                            System.err.printf("QuickTest - Conexión %d - Request %d falló: %s%n", connectionId, j, e.getMessage());
                        }
                    }
                });
            }

            CompletableFuture.allOf(futures).join();

            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            long seconds = Math.max(duration.getSeconds(), 1);

            result.put("testType", testType);
            result.put("concurrentConnections", concurrentConnections);
            result.put("requestsPerConnection", requestsPerConnection);
            result.put("totalExpectedRequests", concurrentConnections * requestsPerConnection);
            result.put("successfulRequests", successfulRequests.get());
            result.put("failedRequests", failedRequests.get());
            result.put("durationSeconds", duration.getSeconds());
            result.put("successRate", (double) successfulRequests.get() / (concurrentConnections * requestsPerConnection) * 100);
            result.put("requestsPerSecond", (double) successfulRequests.get() / seconds);
            result.put("finalStatus", "COMPLETED");
            result.put("endpoint", "load-test-quick");

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("finalStatus", "ERROR");
        } finally {
            activeTests.decrementAndGet();
        }

        return result;
    }
}