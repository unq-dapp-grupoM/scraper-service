package com.dapp.scraper_service.service; // O el paquete que uses

import com.dapp.scraper_service.model.Player;
import com.dapp.scraper_service.model.PlayerMatchStats;
import com.dapp.scraper_service.model.dto.PlayerDTO;
import com.dapp.scraper_service.model.dto.PlayerMatchStatsDTO;
import com.dapp.scraper_service.repository.PlayerRepository;
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
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerService extends AbstractWebService {

    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    // URL de la API interna de búsqueda de WhoScored
    private static final String WHOSCORED_SEARCH_URL = BASE_URL + "search/";

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Cacheable("players")
    public List<PlayerDTO> getPlayerInfoByName(String playerName) {
        // 1. Buscar primero en la base de datos
        List<Player> playersFromDb = playerRepository.findByNameContainingIgnoreCase(playerName);
        if (!playersFromDb.isEmpty()) {
            log.info("{} player(s) found in database for query '{}'. Skipping scrape.", playersFromDb.size(),
                    playerName);
            // Convertir la lista de Entidades a una lista de DTOs y devolverla
            return playersFromDb.stream()
                    .map(this::mapPlayerToDTO)
                    .collect(Collectors.toList());
        }

        log.info("Player '{}' not found in database. Starting scrape.", playerName);
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

            // Guardar en la base de datos
            savePlayer(playerDTO);

            // Devolvemos una lista que contiene el único jugador scrapeado
            return List.of(playerDTO); // Devuelve una lista con el nuevo jugador
        } catch (Exception e) {
            log.error("An unexpected error occurred during scraping for player: {}", playerName, e);
            throw new RuntimeException("An unexpected error occurred while fetching player data.", e);
        }
    }

    private PlayerDTO mapPlayerToDTO(Player player) {
        PlayerDTO dto = new PlayerDTO();
        dto.setName(player.getName());
        dto.setCurrentTeam(player.getCurrentTeam());
        dto.setShirtNumber(player.getShirtNumber());
        dto.setAge(player.getAge());
        dto.setHeight(player.getHeight());
        dto.setNationality(player.getNationality());
        dto.setPositions(player.getPositions());

        List<PlayerMatchStatsDTO> statsDTOs = player.getMatchStats().stream()
                .map(this::mapStatsToDTO)
                .collect(Collectors.toList());
        dto.setMatchStats(statsDTOs);

        return dto;
    }

    private PlayerMatchStatsDTO mapStatsToDTO(PlayerMatchStats stats) {
        // Usamos el builder que ya tienes en el DTO
        return PlayerMatchStatsDTO.builder()
                .opponent(stats.getOpponent()).score(stats.getScore()).date(stats.getDate())
                .position(stats.getPosition()).minsPlayed(stats.getMinsPlayed()).goals(stats.getGoals())
                .assists(stats.getAssists()).yellowCards(stats.getYellowCards()).redCards(stats.getRedCards())
                .shots(stats.getShots()).passSuccess(stats.getPassSuccess()).aerialsWon(stats.getAerialsWon())
                .rating(stats.getRating()).build();
    }

    @Transactional
    protected void savePlayer(PlayerDTO playerDTO) {
        // Usamos orElse para crear uno nuevo si no existe
        Player player = playerRepository.findByNameContainingIgnoreCase(playerDTO.getName()).stream().findFirst()
                .orElse(new Player());

        // Mapear datos del DTO a la Entidad
        player.setName(playerDTO.getName());
        player.setCurrentTeam(playerDTO.getCurrentTeam());
        player.setShirtNumber(playerDTO.getShirtNumber());
        player.setAge(playerDTO.getAge());
        player.setHeight(playerDTO.getHeight());
        player.setNationality(playerDTO.getNationality());
        player.setPositions(playerDTO.getPositions());

        // Limpiar estadísticas viejas para evitar duplicados
        player.getMatchStats().clear();

        // Mapear estadísticas del DTO a la Entidad
        for (PlayerMatchStatsDTO statsDTO : playerDTO.getMatchStats()) {
            PlayerMatchStats stats = new PlayerMatchStats();
            stats.setOpponent(statsDTO.getOpponent());
            stats.setScore(statsDTO.getScore());
            stats.setDate(statsDTO.getDate());
            stats.setPosition(statsDTO.getPosition());
            stats.setMinsPlayed(statsDTO.getMinsPlayed());
            stats.setGoals(statsDTO.getGoals());
            stats.setAssists(statsDTO.getAssists());
            stats.setYellowCards(statsDTO.getYellowCards());
            stats.setRedCards(statsDTO.getRedCards());
            stats.setShots(statsDTO.getShots());
            stats.setPassSuccess(statsDTO.getPassSuccess());
            stats.setAerialsWon(statsDTO.getAerialsWon());
            stats.setRating(statsDTO.getRating());
            stats.setPlayer(player); // Establecer la relación bidireccional
            player.getMatchStats().add(stats);
        }

        playerRepository.save(player);
        log.info("Player '{}' saved or updated in the database.", player.getName());
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
