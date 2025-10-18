// En tu proyecto scraper-service
package com.dapp.scraper_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public abstract class AbstractWebService {

    private static final Logger log = LoggerFactory.getLogger(AbstractWebService.class);
    public static final String BASE_URL = "https://es.whoscored.com/";
    public static final String NOT_FOUND = "Not found";

    private final RestTemplate restTemplate;

    @Value("${scraper.api.key}")
    private String apiKey;

    @Value("${scraper.timeout:30000}")
    private int timeout;

    public AbstractWebService() {
        this.restTemplate = new RestTemplate();
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);
        this.restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);
    }

    public String getHtmlContent(String baseUrl, String searchTerm) {
        log.debug("Searching for: {}", searchTerm);

        String decodedSearch = URLDecoder.decode(searchTerm, StandardCharsets.UTF_8);
        String formattedSearch = decodedSearch.trim().replace(" ", "+");

        String targetUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("t", formattedSearch)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();

        return getHtmlContent(targetUrl, true, "div.search-result");
    }

    public String getHtmlContent(String targetUrl) {
        return getHtmlContent(targetUrl, true, null);
    }

    private String getHtmlContent(String targetUrl, boolean renderJavascript, String waitForSelector) {
        try {
            String encodedTargetUrl = URLEncoder.encode(targetUrl, StandardCharsets.UTF_8.toString());

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://app.scrapingbee.com/api/v1/")
                    .queryParam("api_key", apiKey)
                    .queryParam("url", encodedTargetUrl)
                    .queryParam("render_js", renderJavascript)
                    .queryParam("country_code", "es")
                    .queryParam("timeout", timeout)
                    .queryParam("premium_proxy", "true");
            // Removemos los headers problemáticos por ahora

            if (renderJavascript) {
                builder.queryParam("wait", "2000");
                if (waitForSelector != null && !waitForSelector.isEmpty()) {
                    builder.queryParam("wait_for", waitForSelector);
                }
            }

            URI finalApiUri = builder.build(true).toUri();

            log.debug("Executing ScrapingBee call for: {}", targetUrl);

            String result = restTemplate.getForObject(finalApiUri, String.class);

            if (result == null || result.trim().isEmpty()) {
                log.warn("Empty response from ScrapingBee");
                return NOT_FOUND;
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to scrape URL: {} - Error: {}", targetUrl, e.getMessage());
            throw new RuntimeException("Error during scraping API call for: " + targetUrl, e);
        }
    }
}