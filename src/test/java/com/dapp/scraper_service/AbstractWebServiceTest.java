package com.dapp.scraper_service;

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

import com.dapp.scraper_service.service.AbstractWebService;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractWebServiceTest {

    // Usamos @Spy en una implementación concreta para poder testear la clase
    // abstracta
    @Spy
    private TestWebService webService;

    // Mockeamos RestTemplate para no hacer llamadas HTTP reales
    @Mock
    private RestTemplate restTemplate;

    // Clase de implementación para la prueba
    static class TestWebService extends AbstractWebService {
    }

    @BeforeEach
    void setUp() {
        // Inyectamos el mock de RestTemplate en nuestra instancia de servicio
        ReflectionTestUtils.setField(webService, "restTemplate", restTemplate);
        // Inyectamos los valores que normalmente vendrían de application.properties
        ReflectionTestUtils.setField(webService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(webService, "timeout", 5000);
    }

    @Test
    @DisplayName("Debe construir la URL de ScrapingBee correctamente para una URL simple")
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
        assertFalse(finalUrl.contains("wait_for")); // No debe estar si no se especifica
    }

    @Test
    @DisplayName("Debe construir la URL de ScrapingBee con wait_for para una búsqueda")
    void whenGetHtmlContentForSearch_thenBuildsCorrectScrapingBeeUrlWithWaitFor() throws Exception {
        // Arrange
        String searchTerm = " Lionel Messi "; // Incluimos espacios para probar el trim
        String expectedHtml = "<html><body>Search Result</body></html>";
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(expectedHtml);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

        // Act
        String result = webService.getHtmlContent(AbstractWebService.BASE_URL, searchTerm);

        // Assert
        assertEquals(expectedHtml, result);

        verify(restTemplate).getForObject(uriCaptor.capture(), eq(String.class));
        String finalUrl = uriCaptor.getValue().toString();

        // Verificamos que la URL objetivo (whoscored) se construyó correctamente
        String expectedTargetUrl = "https://es.whoscored.com/?t=Lionel+Messi";
        assertTrue(finalUrl.contains("url=" + URLEncoder.encode(expectedTargetUrl, StandardCharsets.UTF_8.toString())));

        // Verificamos que se añadió el selector de espera
        assertTrue(finalUrl.contains("wait_for=div.search-result"));
    }

    @Test
    @DisplayName("Debe devolver 'Not found' si la respuesta de la API es nula o vacía")
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
    @DisplayName("Debe lanzar una RuntimeException si RestTemplate falla")
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