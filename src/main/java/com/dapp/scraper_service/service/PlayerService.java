package com.dapp.scraper_service.service; // O el paquete que uses

import com.dapp.scraper_service.model.dto.PlayerDTO;
import com.dapp.scraper_service.model.dto.PlayerMatchStatsDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerService extends AbstractWebService {

    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    // URL de la API interna de búsqueda de WhoScored
    private static final String WHOSCORED_SEARCH_URL = BASE_URL + "search/";

    @Cacheable("players")
    public PlayerDTO getPlayerInfoByName(String playerName) {
        try {
            // 1. Scrapear la página de búsqueda. Ahora ScraperAPI manejará la sesión y las
            // cookies.
            String searchResultHtml = getHtmlContent(WHOSCORED_SEARCH_URL, playerName);
            Document searchDoc = Jsoup.parse(searchResultHtml);
            // Usamos el selector que traduce la lógica de Playwright que funcionaba
            Element playerLink = searchDoc
                    .select("div.search-result:has(h2:contains(Jugadores)) tbody tr:nth-child(2) a").first();

            if (playerLink == null) {
                throw new IllegalArgumentException("Player with name '" + playerName + "' not found in search.");
            }

            // 2. Scrapear la página de resumen del jugador
            String playerSummaryUrl = UriComponentsBuilder.fromHttpUrl(BASE_URL).path(playerLink.attr("href"))
                    .toUriString();
            String playerSummaryHtml = getHtmlContent(playerSummaryUrl);
            Document summaryDoc = Jsoup.parse(playerSummaryHtml);

            PlayerDTO playerDTO = scrapePlayerData(summaryDoc);

            // 3. Encontrar el enlace a la página de estadísticas y scrapear esa página
            Element statsLink = summaryDoc.select("a:contains(Estadísticas del Partido)").first();
            if (statsLink != null) {
                String playerStatsUrl = UriComponentsBuilder.fromHttpUrl(BASE_URL).path(statsLink.attr("href"))
                        .toUriString();
                String playerStatsHtml = getHtmlContent(playerStatsUrl);
                Document statsDoc = Jsoup.parse(playerStatsHtml);
                playerDTO.setMatchStats(scrapePlayerMatchStats(statsDoc));
            } else {
                log.warn("Match stats link not found for player '{}'.", playerName);
                playerDTO.setMatchStats(new ArrayList<>());
            }

            return playerDTO;
        } catch (Exception e) {
            log.error("An unexpected error occurred during scraping for player: {}", playerName, e);
            throw new RuntimeException("An unexpected error occurred while fetching player data.", e);
        }
    }

    private PlayerDTO scrapePlayerData(Document doc) {
        PlayerDTO player = new PlayerDTO();
        Element playerInfoContainer = doc.select("div.col12-lg-10.col12-m-10.col12-s-9.col12-xs-8").first();

        if (playerInfoContainer == null) {
            log.error("Player info container not found in the document.");
            throw new IllegalArgumentException("Could not parse player data page for the given player.");
        }

        player.setName(extractValueFromPlayerInfo(playerInfoContainer, "Nombre"));
        player.setCurrentTeam(extractValueFromPlayerInfo(playerInfoContainer, "Equipo Actual"));
        player.setShirtNumber(extractValueFromPlayerInfo(playerInfoContainer, "Número de Dorsal"));
        player.setAge(extractValueFromPlayerInfo(playerInfoContainer, "Edad").split(" ")[0].trim());
        player.setHeight(extractValueFromPlayerInfo(playerInfoContainer, "Altura"));
        player.setNationality(extractValueFromPlayerInfo(playerInfoContainer, "Nacionalidad"));
        player.setPositions(extractPlayerPositionsFromPlayerInfo(playerInfoContainer));

        return player;
    }

    private String extractValueFromPlayerInfo(Element context, String label) {
        try {
            // Selector para encontrar el div que contiene el label y el valor
            Element infoDiv = context.select(String.format("div.col12-lg-6:has(span.info-label:contains(%s:))", label))
                    .first();
            if (infoDiv != null) {
                return infoDiv.text().replace(label + ":", "").trim();
            }
            return NOT_FOUND;
        } catch (Exception e) {
            log.warn("Could not extract value for label '{}'", label);
            return NOT_FOUND;
        }
    }

    private String extractPlayerPositionsFromPlayerInfo(Element playerInfoContainer) {
        Elements positionsSpans = playerInfoContainer
                .select("div:has(span.info-label:contains(Posiciones:)) > span:not(.info-label) span");
        if (!positionsSpans.isEmpty()) {
            return positionsSpans.stream()
                    .map(Element::text)
                    .collect(Collectors.joining(" "));
        }
        return NOT_FOUND;
    }

    private List<PlayerMatchStatsDTO> scrapePlayerMatchStats(Document doc) {
        List<PlayerMatchStatsDTO> matchStats = new ArrayList<>();
        Elements statsRows = doc.select("tbody#player-table-statistics-body tr");

        for (Element row : statsRows) {
            PlayerMatchStatsDTO match = PlayerMatchStatsDTO.builder()
                    .opponent(row.select("td:nth-child(1) a.player-match-link").text().split("\n")[0])
                    .score(row.select("td:nth-child(1) span.scoreline").text())
                    .date(row.select("td:nth-child(3)").text())
                    .position(row.select("td:nth-child(4)").text())
                    .minsPlayed(row.select("td:nth-child(5)").text())
                    .goals(row.select("td:nth-child(6)").text())
                    .assists(row.select("td:nth-child(7)").text())
                    .yellowCards(row.select("td:nth-child(8)").text())
                    .redCards(row.select("td:nth-child(9)").text())
                    .shots(row.select("td:nth-child(10)").text())
                    .passSuccess(row.select("td:nth-child(11)").text())
                    .aerialsWon(row.select("td:nth-child(12)").text())
                    .rating(row.select("td:nth-child(13)").text())
                    .build();
            matchStats.add(match);
        }
        return matchStats;
    }
}
