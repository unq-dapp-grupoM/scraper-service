// En tu proyecto scraper-service
package com.dapp.scraper_service.service; // O el paquete que uses

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.dapp.scraper_service.model.dto.TeamDTO;
import com.dapp.scraper_service.model.dto.TeamPlayerDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;

@Service
public class TeamService extends AbstractWebService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private static final String WHOSCORED_SEARCH_URL = BASE_URL + "search/";

    @Cacheable("teams")
    public TeamDTO getTeamInfoByName(String teamName) {
        try {
            // 1. Buscar el equipo para obtener su URL
            String searchPageHtml = getHtmlContent(WHOSCORED_SEARCH_URL, teamName);
            Document searchDoc = Jsoup.parse(searchPageHtml);

            // Selector de JSoup para encontrar el enlace del equipo
            Element teamLink = searchDoc.select("div.search-result:has(h2:contains(Equipos)) tbody tr:nth-child(2) a")
                    .first();
            if (teamLink == null) {
                throw new IllegalArgumentException("Team with name '" + teamName + "' not found in search.");
            }

            String teamPageUrl = UriComponentsBuilder.fromHttpUrl(BASE_URL).path(teamLink.attr("href"))
                    .toUriString();

            // 2. Scrapear la página del equipo
            String teamPageHtml = getHtmlContent(teamPageUrl);
            Document teamDoc = Jsoup.parse(teamPageHtml);

            TeamDTO teamDTO = new TeamDTO();
            teamDTO.setName(teamDoc.select("h1.team-header").text());
            teamDTO.setSquad(scrapeSquadData(teamDoc));

            return teamDTO;

        } catch (Exception e) {
            log.error("An error occurred during scraping for team: {}", teamName, e);
            throw new RuntimeException("An unexpected error occurred while fetching team data.", e);
        }
    }

    private List<TeamPlayerDTO> scrapeSquadData(Document doc) {
        List<TeamPlayerDTO> squad = new ArrayList<>();
        Elements playerRows = doc.select("tbody#player-table-statistics-body tr");

        for (Element row : playerRows) {
            TeamPlayerDTO player = TeamPlayerDTO.builder()
                    .name(row.select("td:nth-child(1) a.player-link span.iconize-icon-left").text())
                    .age(row.select("td:nth-child(1) span.player-meta-data:nth-of-type(1)").text())
                    .position(row.select("td:nth-child(1) span.player-meta-data:nth-of-type(2)").text()
                            .replace(",", "").trim())
                    .height(row.select("td:nth-child(3)").text())
                    .weight(row.select("td:nth-child(4)").text())
                    .apps(row.select("td:nth-child(5)").text())
                    .minsPlayed(row.select("td:nth-child(6)").text())
                    .goals(row.select("td:nth-child(7)").text())
                    .assists(row.select("td:nth-child(8)").text())
                    .yellowCards(row.select("td:nth-child(9)").text())
                    .redCards(row.select("td:nth-child(10)").text())
                    .shotsPerGame(row.select("td:nth-child(11)").text())
                    .passSuccess(row.select("td:nth-child(12)").text())
                    .aerialsWonPerGame(row.select("td:nth-child(13)").text())
                    .manOfTheMatch(row.select("td:nth-child(14)").text())
                    .rating(row.select("td:nth-child(15)").text())
                    .build();
            squad.add(player);
        }
        return squad;
    }
}
