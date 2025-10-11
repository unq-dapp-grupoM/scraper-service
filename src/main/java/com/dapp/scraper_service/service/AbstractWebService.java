// En tu proyecto scraper-service
package com.dapp.scraper_service.service; // O el paquete que uses

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
public abstract class AbstractWebService {

    private static final Logger log = LoggerFactory.getLogger(AbstractWebService.class);
    protected static final String BASE_URL = "https://es.whoscored.com/";
    protected static final String NOT_FOUND = "Not found";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${scraper.api.key}")
    private String apiKey;

    // Sobrecargamos el método para búsquedas
    protected String getHtmlContent(String baseUrl, String searchTerm) {
        log.debug(searchTerm);
        // WhoScored AJAX espera '+' en lugar de '%20' para los espacios
        String decodedSearch = URLDecoder.decode(searchTerm, StandardCharsets.UTF_8);
        String formattedSearch = decodedSearch.trim().replace(" ", "+");

        String targetUrl = UriComponentsBuilder.fromHttpUrl(baseUrl).queryParam("t", formattedSearch).toUriString();
        return getHtmlContent(targetUrl, true, null);
    }

    // Método para obtener páginas que sí necesitan renderizado de JS
    protected String getHtmlContent(String targetUrl) {
        return getHtmlContent(targetUrl, true, null);
    }

    private String getHtmlContent(String targetUrl, boolean renderJavascript, HttpHeaders customHeaders) {
        // Volvemos a la construcción manual de la URL, como en la documentación de
        // ScraperAPI,
        // para tener control total sobre la codificación.
        StringBuilder apiUrlBuilder = new StringBuilder("http://api.scraperapi.com?");
        apiUrlBuilder.append("api_key={apiKey}");
        apiUrlBuilder.append("&session_number={sessionNum}");
        apiUrlBuilder.append("&country_code=ar");

        if (renderJavascript) {
            apiUrlBuilder.append("&render=true");
        }
        // La parte crucial: codificamos la URL de destino y la añadimos.
        apiUrlBuilder.append("&url={targetUrl}");

        URI finalApiUri = UriComponentsBuilder.fromUriString(apiUrlBuilder.toString())
                .build(apiKey, targetUrl.hashCode(), targetUrl);

        log.debug("Executing ScraperAPI call for URI: {} with JS rendering: {} and custom headers: {}", finalApiUri,
                renderJavascript, customHeaders != null);

        HttpEntity<String> requestEntity = new HttpEntity<>(customHeaders);
        String htmlContent = restTemplate.exchange(finalApiUri, HttpMethod.GET, requestEntity, String.class).getBody();

        return htmlContent;
    }
}
