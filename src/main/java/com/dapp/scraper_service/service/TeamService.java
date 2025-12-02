// In your scraper-service project
package com.dapp.scraper_service.service;

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

    public TeamService(TeamRepository teamRepository, MatchRepository matchRepository) {
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
    }

    @Cacheable("teams")
    public List<TeamDTO> getTeamInfoByName(String teamName) {
        // 1. First, search in the database
        List<Team> teamsFromDb = teamRepository.findByNameContainingIgnoreCase(teamName);
        if (!teamsFromDb.isEmpty()) {
            log.info("{} team(s) found in database for query '{}'. Skipping scrape.", teamsFromDb.size(), teamName);
            // Convert the list of Entities to a list of DTOs and return it
            return teamsFromDb.stream()
                    .map(this::mapTeamToDTO)
                    .collect(Collectors.toList());
        }

        log.info("Team '{}' not found in database. Starting scrape.", teamName);
        try {
            // 1. Search for the team to get its URL
            String searchPageHtml = getHtmlContent(WHOSCORED_SEARCH_URL, teamName);
            Document searchDoc = Jsoup.parse(searchPageHtml);

            // JSoup selector to find the team link
            Element teamLink = searchDoc.select("div.search-result:has(h2:contains(Equipos)) tbody tr:nth-child(2) a")
                    .first();
            if (teamLink == null) {
                throw new IllegalArgumentException("Team with name '" + teamName + "' not found in search.");
            }

            String teamPageUrl = UriComponentsBuilder.fromHttpUrl(BASE_URL).path(teamLink.attr("href"))
                    .toUriString();

            // 2. Scrape the team page
            String teamPageHtml = getHtmlContent(teamPageUrl);
            Document teamDoc = Jsoup.parse(teamPageHtml);

            TeamDTO teamDTO = new TeamDTO();
            teamDTO.setName(teamDoc.select("h1.team-header").text());
            teamDTO.setSquad(scrapeSquadData(teamDoc));

            // Save to the database
            saveTeam(teamDTO);

            return List.of(teamDTO); // Return a list with the new team

        } catch (Exception e) {
            log.error("An error occurred during scraping for team: {}", teamName, e);
            throw new RuntimeException("An unexpected error occurred while fetching team data.", e);
        }
    }

    @Cacheable("future-matches")
    public List<MatchDTO> getFutureMatchesByTeamName(String teamName) {
        // 1. Search for the team in the DB
        log.info("Scraping future matches for team '{}'.", teamName);
        Team team = teamRepository.findByNameContainingIgnoreCase(teamName).stream().findFirst().orElse(null);

        // If the team exists, we search for its matches in the MatchRepository
        if (team != null) {
            List<Match> matchesFromDb = matchRepository.findByTeam(team);
            if (!matchesFromDb.isEmpty()) {
                // If there are matches in the database, we return them
                log.info("Future matches for team '{}' found in database. Skipping scrape.", teamName);
                return matchesFromDb.stream().map(this::mapMatchToDTO).collect(Collectors.toList());
            }
        }

        log.info("Future matches for team '{}' not found in database. Starting scrape.", teamName);
        try {
            // 1. Search for the team to get its URL
            String searchPageHtml = getHtmlContent(WHOSCORED_SEARCH_URL, teamName);
            Document searchDoc = Jsoup.parse(searchPageHtml);

            Element teamLink = searchDoc.select("div.search-result:has(h2:contains(Equipos)) tbody tr:nth-child(2) a")
                    .first();
            if (teamLink == null) {
                throw new IllegalArgumentException("Team with name '" + teamName + "' not found in search.");
            }

            String teamPageUrl = UriComponentsBuilder.fromHttpUrl(BASE_URL).path(teamLink.attr("href")).toUriString();

            // 2. Scrape the team summary page to find the link to "Fixtures"
            String teamPageHtml = getHtmlContent(teamPageUrl);
            Document teamDoc = Jsoup.parse(teamPageHtml);

            // If the team doesn't exist in the database, we save it first
            if (team == null) {
                TeamDTO teamDTO = new TeamDTO();
                teamDTO.setName(teamDoc.select("h1.team-header").text());
                teamDTO.setSquad(scrapeSquadData(teamDoc));

                saveTeam(teamDTO);
            }

            // Search for the link to the fixtures section
            Element fixtureLink = teamDoc.selectFirst("a:contains(Encuentros)");
            if (fixtureLink == null) {
                log.warn("Fixtures link not found for team '{}'.", teamName);
                throw new RuntimeException("Could not find fixtures link for team: " + teamName);
            }

            // 3. Scrape the fixtures page
            String fixturePageUrl = UriComponentsBuilder.fromHttpUrl(BASE_URL).path(fixtureLink.attr("href"))
                    .toUriString();
            String fixturePageHtml = getHtmlContent(fixturePageUrl);
            Document fixtureDoc = Jsoup.parse(fixturePageHtml);

            return parseAndSaveFutureMatches(fixtureDoc, teamName);

        } catch (IllegalArgumentException e) {
            log.warn("Could not find team '{}'.", teamName, e); // Re-throw so the controller can handle it as a 404
            throw e;
        } catch (Exception e) {
            log.error("An error occurred during scraping for future matches of team: {}", teamName, e);
            throw new RuntimeException("An unexpected error occurred while fetching future matches.", e);
        }
    }

    @Transactional
    public List<MatchDTO> parseAndSaveFutureMatches(Document doc, String requestedTeamName) {
        // Log to print the full HTML content received
        log.debug("Full HTML content received for parsing future matches:\n{}", doc.outerHtml());

        List<Match> futureMatchesToSave = new ArrayList<>();
        Team team = teamRepository.findByNameContainingIgnoreCase(requestedTeamName).stream().findFirst().orElse(null);

        if (team != null) {
            // We clear old matches using the repository to avoid outdated data
            matchRepository.deleteByTeam(team);
        } else {
            log.warn("Team '{}' not found in DB. Matches will be returned but not saved.", requestedTeamName);
        }

        // The pattern to extract the JS array containing match data
        Pattern pattern = Pattern.compile("fixtureMatches: (?<matches>\\[\\[.*?)\\]\\s*};", Pattern.DOTALL);
        // We search in all the scripts on the page
        for (Element script : doc.select("script")) {
            Matcher matcher = pattern.matcher(script.html());
            if (matcher.find()) {
                log.info("Found 'fixtureMatches' data script.");
                String matchesData = matcher.group("matches");
                matchesData = matchesData.replaceAll("\\n", "") // Remove newlines
                        .replaceAll("'", "") // Remove single quotes
                        .substring(1); // Remove the first bracket
                String[] matches = matchesData.split("\\],\\[");
                log.debug("Found {} potential matches in the script data.", matches.length);

                for (String matchStr : matches) {
                    String[] fields = matchStr.split(",");
                    // Field 10 is the result. If it's 'vs', it's a future match.
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
                // Once we find the script, we don't need to search anymore.
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
    public void saveTeam(TeamDTO teamDTO) {
        Team team = teamRepository.findByNameContainingIgnoreCase(teamDTO.getName()).stream().findFirst()
                .orElse(new Team());

        team.setName(teamDTO.getName());

        // Clear old squad to avoid duplicates
        team.getSquad().clear();

        // Map player DTOs to Entities
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
            player.setTeam(team); // Set the bidirectional relationship
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
        // Indices based on the JS array from the example HTML
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
