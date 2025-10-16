// En tu proyecto scraper-service
package com.dapp.scraper_service.service; // O el paquete que uses

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.dapp.scraper_service.model.Team;
import com.dapp.scraper_service.model.TeamPlayer;
import com.dapp.scraper_service.model.dto.TeamDTO;
import com.dapp.scraper_service.model.dto.TeamPlayerDTO;
import com.dapp.scraper_service.repository.TeamRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

@Service
public class TeamService extends AbstractWebService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private static final String WHOSCORED_SEARCH_URL = BASE_URL + "search/";

    private final TeamRepository teamRepository;

    @Autowired
    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Cacheable("teams")
    public List<TeamDTO> getTeamInfoByName(String teamName) {
        // 1. Buscar primero en la base de datos
        List<Team> teamsFromDb = teamRepository.findByNameContainingIgnoreCase(teamName);
        if (!teamsFromDb.isEmpty()) {
            log.info("{} team(s) found in database for query '{}'. Skipping scrape.", teamsFromDb.size(), teamName);
            // Convertir la lista de Entidades a una lista de DTOs y devolverla
            return teamsFromDb.stream()
                    .map(this::mapTeamToDTO)
                    .collect(Collectors.toList());
        }

        log.info("Team '{}' not found in database. Starting scrape.", teamName);
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

            // Guardar en la base de datos
            saveTeam(teamDTO);

            return List.of(teamDTO); // Devuelve una lista con el nuevo equipo

        } catch (Exception e) {
            log.error("An error occurred during scraping for team: {}", teamName, e);
            throw new RuntimeException("An unexpected error occurred while fetching team data.", e);
        }
    }

    private TeamDTO mapTeamToDTO(Team team) {
        TeamDTO dto = new TeamDTO();
        dto.setName(team.getName());

        List<TeamPlayerDTO> squadDTO = team.getSquad().stream()
                .map(this::mapTeamPlayerToDTO)
                .collect(Collectors.toList());
        dto.setSquad(squadDTO);

        return dto;
    }

    private TeamPlayerDTO mapTeamPlayerToDTO(TeamPlayer player) {
        return TeamPlayerDTO.builder()
                .name(player.getName())
                .age(player.getAge())
                .position(player.getPosition())
                .height(player.getHeight())
                .weight(player.getWeight())
                .apps(player.getApps())
                .minsPlayed(player.getMinsPlayed())
                .goals(player.getGoals())
                .assists(player.getAssists())
                .yellowCards(player.getYellowCards())
                .redCards(player.getRedCards())
                .shotsPerGame(player.getShotsPerGame())
                .passSuccess(player.getPassSuccess())
                .aerialsWonPerGame(player.getAerialsWonPerGame())
                .manOfTheMatch(player.getManOfTheMatch())
                .rating(player.getRating())
                .build();
    }

    @Transactional
    protected void saveTeam(TeamDTO teamDTO) {
        Team team = teamRepository.findByNameContainingIgnoreCase(teamDTO.getName()).stream().findFirst()
                .orElse(new Team());

        team.setName(teamDTO.getName());

        // Limpiar plantilla vieja para evitar duplicados
        team.getSquad().clear();

        // Mapear DTOs de jugadores a Entidades
        for (TeamPlayerDTO playerDTO : teamDTO.getSquad()) {
            TeamPlayer player = new TeamPlayer();
            player.setName(playerDTO.getName());
            player.setAge(playerDTO.getAge());
            player.setPosition(playerDTO.getPosition());
            player.setHeight(playerDTO.getHeight());
            player.setWeight(playerDTO.getWeight());
            player.setApps(playerDTO.getApps());
            player.setMinsPlayed(playerDTO.getMinsPlayed());
            player.setGoals(playerDTO.getGoals());
            player.setAssists(playerDTO.getAssists());
            player.setYellowCards(playerDTO.getYellowCards());
            player.setRedCards(playerDTO.getRedCards());
            player.setShotsPerGame(playerDTO.getShotsPerGame());
            player.setPassSuccess(playerDTO.getPassSuccess());
            player.setAerialsWonPerGame(playerDTO.getAerialsWonPerGame());
            player.setManOfTheMatch(playerDTO.getManOfTheMatch());
            player.setRating(playerDTO.getRating());
            player.setTeam(team); // Establecer la relación bidireccional
            team.getSquad().add(player);
        }

        teamRepository.save(team);
        log.info("Team '{}' saved or updated in the database.", team.getName());
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
