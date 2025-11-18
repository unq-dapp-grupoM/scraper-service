package com.dapp.scraper_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractWebServiceTest {

    // We use @Spy on a concrete implementation to test the abstract class
    @Spy
    private TestWebService webService;

    // We mock RestTemplate to avoid making real HTTP calls
    @Mock
    private RestTemplate restTemplate;

    // Implementation class for the test
    static class TestWebService extends AbstractWebService {
    }

    @BeforeEach
    void setUp() {
        // We inject the RestTemplate mock into our service instance
        ReflectionTestUtils.setField(webService, "restTemplate", restTemplate);
        // We inject the values that would normally come from application.properties
        ReflectionTestUtils.setField(webService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(webService, "timeout", 5000);
    }

    @Test
    @DisplayName("Should build the ScrapingBee URL correctly for a simple URL")
    void whenGetHtmlContentForUrl_thenBuildsCorrectScrapingBeeUrl() throws Exception {
        // Arrange
        String targetUrl = "https://example.com/player/123";
        String expectedHtml = "<html><body>Test</body></html>";
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(expectedHtml);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

        // Act
        String result = webService.getHtmlContent(targetUrl);

        // Assert
        assertEquals(expectedHtml, result);

        verify(restTemplate).getForObject(uriCaptor.capture(), eq(String.class));
        String finalUrl = uriCaptor.getValue().toString();

        assertTrue(finalUrl.startsWith("https://app.scrapingbee.com/api/v1/"));
        assertTrue(finalUrl.contains("api_key=test-api-key"));
        assertTrue(finalUrl.contains("url=" + URLEncoder.encode(targetUrl, StandardCharsets.UTF_8.toString())));
        assertTrue(finalUrl.contains("render_js=true"));
        assertTrue(finalUrl.contains("country_code=es"));
        assertTrue(finalUrl.contains("timeout=5000"));
        assertTrue(finalUrl.contains("premium_proxy=true"));
        assertTrue(finalUrl.contains("wait=2000"));
        assertFalse(finalUrl.contains("wait_for")); // Should not be present if not specified
    }

    @Test
    @DisplayName("Should build the ScrapingBee URL with wait_for for a search")
    void whenGetHtmlContentForSearch_thenBuildsCorrectScrapingBeeUrlWithWaitFor() throws Exception {
        // Arrange
        String searchTerm = " Lionel Messi "; // We include spaces to test the trim
        String expectedHtml = "<html><body>Search Result</body></html>";
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(expectedHtml);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

        // Act
        String result = webService.getHtmlContent(AbstractWebService.BASE_URL, searchTerm);

        // Assert
        assertEquals(expectedHtml, result);

        verify(restTemplate).getForObject(uriCaptor.capture(), eq(String.class));
        String finalUrl = uriCaptor.getValue().toString();

        // We verify that the target URL (whoscored) was built correctly
        String expectedTargetUrl = "https://es.whoscored.com/?t=Lionel+Messi";
        assertTrue(finalUrl.contains("url=" + URLEncoder.encode(expectedTargetUrl, StandardCharsets.UTF_8.toString())));

        // We verify that the wait selector was added
        assertTrue(finalUrl.contains("wait_for=div.search-result"));
    }

    @Test
    @DisplayName("Should return 'Not found' if the API response is null or empty")
    void whenApiResponseIsNull_thenReturnNotFoundConstant() {
        // Arrange
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(null);

        // Act
        String result = webService.getHtmlContent("https://example.com");

        // Assert
        assertEquals(AbstractWebService.NOT_FOUND, result);

        // Arrange 2: Empty string
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn("   ");

        // Act 2
        result = webService.getHtmlContent("https://example.com");

        // Assert 2
        assertEquals(AbstractWebService.NOT_FOUND, result);
    }

    @Test
    @DisplayName("Should throw a RuntimeException if RestTemplate fails")
    void whenRestTemplateThrowsException_thenThrowRuntimeException() {
        // Arrange
        String targetUrl = "https://failing-url.com";
        when(restTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            webService.getHtmlContent(targetUrl);
        });

        assertTrue(exception.getMessage().contains("Error during scraping API call for: " + targetUrl));
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof org.springframework.web.client.HttpClientErrorException);
    }
}