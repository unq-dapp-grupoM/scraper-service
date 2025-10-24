// En tu proyecto scraper-service
package com.dapp.scraper_service.service; // O el paquete que uses

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.dapp.scraper_service.model.Match;
import com.dapp.scraper_service.model.Team;
import com.dapp.scraper_service.model.TeamPlayer;
import com.dapp.scraper_service.model.dto.MatchDTO;
import com.dapp.scraper_service.model.dto.TeamDTO;
import com.dapp.scraper_service.model.dto.TeamPlayerDTO;
import com.dapp.scraper_service.repository.MatchRepository;
import com.dapp.scraper_service.repository.TeamRepository;

@Service
public class TeamService extends AbstractWebService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private static final String WHOSCORED_SEARCH_URL = BASE_URL + "search/";

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;

    @Autowired
    public TeamService(TeamRepository teamRepository, MatchRepository matchRepository) {
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
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

    @Cacheable("future-matches")
    public List<MatchDTO> getFutureMatchesByTeamName(String teamName) {
        // 1. Buscar el equipo en la BD
        log.info("Scraping future matches for team '{}'.", teamName);
        Team team = teamRepository.findByNameContainingIgnoreCase(teamName).stream().findFirst().orElse(null);

        // Si el equipo existe, buscamos sus partidos en el MatchRepository
        if (team != null) {
            List<Match> matchesFromDb = matchRepository.findByTeam(team);
            if (!matchesFromDb.isEmpty()) {
                // Si hay partidos/matches en la base de datos, los devolvemos
                log.info("Future matches for team '{}' found in database. Skipping scrape.", teamName);
                return matchesFromDb.stream().map(this::mapMatchToDTO).collect(Collectors.toList());
            }
        }

        log.info("Future matches for team '{}' not found in database. Starting scrape.", teamName);
        try {
            // 1. Buscar el equipo para obtener su URL
            String searchPageHtml = getHtmlContent(WHOSCORED_SEARCH_URL, teamName);
            Document searchDoc = Jsoup.parse(searchPageHtml);

            Element teamLink = searchDoc.select("div.search-result:has(h2:contains(Equipos)) tbody tr:nth-child(2) a")
                    .first();
            if (teamLink == null) {
                throw new IllegalArgumentException("Team with name '" + teamName + "' not found in search.");
            }

            String teamPageUrl = UriComponentsBuilder.fromHttpUrl(BASE_URL).path(teamLink.attr("href")).toUriString();

            // 2. Scrapear la página de resumen del equipo para encontrar el enlace a "Encuentros"
            String teamPageHtml = getHtmlContent(teamPageUrl);
            Document teamDoc = Jsoup.parse(teamPageHtml);

            // Si el equipo no existe en la base de datos, lo guardamos primero
            if(team == null) {
                TeamDTO teamDTO = new TeamDTO();
                teamDTO.setName(teamDoc.select("h1.team-header").text());
                teamDTO.setSquad(scrapeSquadData(teamDoc));
                
                saveTeam(teamDTO);
            }

            // Buscar el enlace a la sección de encuentros/fixtures
            Element fixtureLink = teamDoc.selectFirst("a:contains(Encuentros)");
            if (fixtureLink == null) {
                log.warn("Fixtures link not found for team '{}'.", teamName);
                throw new RuntimeException("Could not find fixtures link for team: " + teamName);
            }

            // 3. Scrapear la página de encuentros
            String fixturePageUrl = UriComponentsBuilder.fromHttpUrl(BASE_URL).path(fixtureLink.attr("href")).toUriString();
            String fixturePageHtml = getHtmlContent(fixturePageUrl);
            Document fixtureDoc = Jsoup.parse(fixturePageHtml);

            return parseAndSaveFutureMatches(fixtureDoc, teamName);

        } catch (IllegalArgumentException e) {
            log.warn("Could not find team '{}'.", teamName, e);
            throw e; // Re-lanzar para que el controller lo maneje como 404
        } catch (Exception e) {
            log.error("An error occurred during scraping for future matches of team: {}", teamName, e);
            throw new RuntimeException("An unexpected error occurred while fetching future matches.", e);
        }
    }

    @Transactional
    private List<MatchDTO> parseAndSaveFutureMatches(Document doc, String requestedTeamName) {
        // Log para imprimir el contenido completo del HTML recibido
        log.debug("Full HTML content received for parsing future matches:\n{}", doc.outerHtml());
        
        List<Match> futureMatchesToSave = new ArrayList<>();
        Team team = teamRepository.findByNameContainingIgnoreCase(requestedTeamName).stream().findFirst().orElse(null);
        
        if (team != null) {
            // Limpiamos los partidos viejos usando el repositorio para evitar datos obsoletos
            matchRepository.deleteByTeam(team);
        } else {
            log.warn("Team '{}' not found in DB. Matches will be returned but not saved.", requestedTeamName);
        }

        // El patrón para extraer el array de JS que contiene los datos de los partidos
        Pattern pattern = Pattern.compile("fixtureMatches: (?<matches>\\[\\[.*?)\\]\\s*};", Pattern.DOTALL);
        // Buscamos en todos los scripts de la página
        for (Element script : doc.select("script")) {
            Matcher matcher = pattern.matcher(script.html());
            if (matcher.find()) {
                log.info("Found 'fixtureMatches' data script.");
                String matchesData = matcher.group("matches");
                matchesData = matchesData.replaceAll("\\n", "") // Eliminar saltos de línea
                    .replaceAll("'", "") // Eliminar comillas simples
                    .substring(1); // Eliminar el primer corchete
                String[] matches = matchesData.split("\\],\\[");
                log.debug("Found {} potential matches in the script data.", matches.length);

                for (String matchStr : matches) {
                    String[] fields = matchStr.split(",");
                    // El campo 10 es el resultado. Si es 'vs', es un partido futuro.
                    if (fields.length > 10 && "vs".equals(fields[10].trim())) {
                        log.debug("Found future match: {} vs {} on {}", fields[5].trim(), fields[8].trim(),
                                fields[2].trim());
                        MatchDTO dto = mapFieldsToMatchDTO(fields);
                        if (team != null) {
                            Match matchEntity = mapDTOToMatch(dto);
                            matchEntity.setTeam(team);
                            futureMatchesToSave.add(matchEntity);
                        }
                    } else {
                        log.trace("Skipping past match or malformed data row.");
                    }
                }
                // Una vez que encontramos el script, no necesitamos seguir buscando.
                break;
            }
        }
        if (!futureMatchesToSave.isEmpty()) {
            matchRepository.saveAll(futureMatchesToSave);
            log.info("Saved {} future matches for team '{}' in the database.", futureMatchesToSave.size(),
                    team.getName());
        }
        return futureMatchesToSave.stream().map(this::mapMatchToDTO).collect(Collectors.toList());
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

    private MatchDTO mapFieldsToMatchDTO(String[] fields) {
        // Índices basados en el array de JS del HTML de ejemplo
        return MatchDTO.builder()
                .homeTeam(fields[5].trim())
                .awayTeam(fields[8].trim())
                .date(fields[2].trim())
                .competition(fields[16].trim())
                .build();
    }

    private MatchDTO mapMatchToDTO(Match match) {
        return MatchDTO.builder()
                .homeTeam(match.getHomeTeam())
                .awayTeam(match.getAwayTeam())
                .date(match.getDate())
                .competition(match.getCompetition())
                .build();
    }

    private Match mapDTOToMatch(MatchDTO dto) {
        Match match = new Match();
        match.setHomeTeam(dto.getHomeTeam());
        match.setAwayTeam(dto.getAwayTeam());
        match.setDate(dto.getDate());
        match.setCompetition(dto.getCompetition());
        return match;
    }
}
