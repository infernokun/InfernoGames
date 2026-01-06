package com.infernokun.infernoGames.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infernokun.infernoGames.config.InfernoGamesConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IGDBService {

    private static final String IGDB_API_URL = "https://api.igdb.com/v4";
    private static final String TWITCH_AUTH_URL = "https://id.twitch.tv/oauth2/token";

    private final InfernoGamesConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String accessToken;
    private long tokenExpiresAt;

    private final SteamService steamService;

    public IGDBService(InfernoGamesConfig config, SteamService steamService) {
        this.config = config;
        this.steamService = steamService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Authenticate with Twitch to get IGDB access token
     */
    private void authenticate() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt - 60000) {
            return; // Token is still valid
        }

        try {
            String authUrl = String.format("%s?client_id=%s&client_secret=%s&grant_type=client_credentials",
                    TWITCH_AUTH_URL, config.getIgdbClientId(), config.getIgdbClientSecret());

            ResponseEntity<TwitchAuthResponse> response = restTemplate.postForEntity(
                    authUrl, null, TwitchAuthResponse.class);

            if (response.getBody() != null) {
                accessToken = response.getBody().getAccessToken();
                tokenExpiresAt = System.currentTimeMillis() + (response.getBody().getExpiresIn() * 1000);
                log.info("Successfully authenticated with IGDB/Twitch API");
            }
        } catch (Exception e) {
            log.error("Failed to authenticate with IGDB: {}", e.getMessage());
            throw new RuntimeException("IGDB authentication failed", e);
        }
    }

    /**
     * Search for games by name
     */
    @Cacheable(value = "igdbSearch", key = "#query")
    public List<IGDBGameDto> searchGames(String query) {
        authenticate();

        String body = String.format(
                "search \"%s\"; " +
                        "fields id,name,summary,cover.url,first_release_date,genres.name,platforms.name," +
                        "involved_companies.company.name,involved_companies.developer,involved_companies.publisher," +
                        "rating,rating_count,screenshots.url,url,external_games.category,external_games.uid,external_games.name; " +
                        "limit 20;",
                query.replace("\"", "\\\"")
        );

        return executeIGDBRequest(body);
    }

    /**
     * Get game details by IGDB ID
     */
    @Cacheable(value = "igdbGame", key = "#igdbId")
    public Optional<IGDBGameDto> getGameById(Long igdbId) {
        authenticate();

        String body = String.format(
                "where id = %d; " +
                        "fields id,name,summary,storyline,cover.url,first_release_date,genres.name,platforms.name," +
                        "involved_companies.company.name,involved_companies.developer,involved_companies.publisher," +
                        "rating,rating_count,screenshots.url,url,videos.video_id,websites.url,websites.category," +
                        "similar_games.name,similar_games.cover.url,aggregated_rating,aggregated_rating_count," +
                        "external_games.category,external_games.uid,external_games.name;",
                igdbId
        );

        List<IGDBGameDto> results = executeIGDBRequest(body);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * Get popular/trending games
     */
    public List<IGDBGameDto> getPopularGames(int limit) {
        authenticate();

        String body = String.format(
                "fields id,name,summary,cover.url,first_release_date,genres.name,platforms.name," +
                        "involved_companies.company.name,involved_companies.developer,involved_companies.publisher," +
                        "rating,rating_count,screenshots.url,url,external_games.category,external_games.uid,external_games.name; " +
                        "sort rating desc; " +
                        "where rating_count > 100; " +
                        "limit %d;",
                limit
        );

        return executeIGDBRequest(body);
    }

    /**
     * Get recently released games
     */
    public List<IGDBGameDto> getRecentReleases(int limit) {
        authenticate();

        long now = Instant.now().getEpochSecond();

        String body = String.format(
                "fields id,name,summary,cover.url,first_release_date,genres.name,platforms.name," +
                        "involved_companies.company.name,involved_companies.developer,involved_companies.publisher," +
                        "rating,rating_count,screenshots.url,url,external_games.category,external_games.uid,external_games.name; " +
                        "sort first_release_date desc; " +
                        "where first_release_date < %d & first_release_date != null; " +
                        "limit %d;",
                now, limit
        );

        return executeIGDBRequest(body);
    }

    /**
     * Get upcoming games
     */
    public List<IGDBGameDto> getUpcomingGames(int limit) {
        authenticate();

        long now = Instant.now().getEpochSecond();

        String body = String.format(
                "fields id,name,summary,cover.url,first_release_date,genres.name,platforms.name," +
                        "involved_companies.company.name,involved_companies.developer,involved_companies.publisher," +
                        "rating,rating_count,screenshots.url,url,external_games.category,external_games.uid,external_games.name; " +
                        "sort first_release_date asc; " +
                        "where first_release_date > %d; " +
                        "limit %d;",
                now, limit
        );

        return executeIGDBRequest(body);
    }

    /**
     * Execute IGDB API request
     */
    private List<IGDBGameDto> executeIGDBRequest(String body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Client-ID", config.getIgdbClientId());
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    IGDB_API_URL + "/games",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getBody() != null) {
                List<IGDBRawGame> rawGames = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<>() {
                        }
                );

                return rawGames.stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList());
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("IGDB API request failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Convert raw IGDB response to DTO
     */
    private IGDBGameDto convertToDto(IGDBRawGame raw) {
        IGDBGameDto dto = new IGDBGameDto();
        dto.setIgdbId(raw.getId());
        dto.setName(raw.getName());
        dto.setSummary(raw.getSummary());
        dto.setStoryline(raw.getStoryline());
        dto.setUrl(raw.getUrl());
        dto.setRating(raw.getRating());
        dto.setRatingCount(raw.getRatingCount());
        dto.setAggregatedRating(raw.getAggregatedRating());

        // Convert cover URL to full size
        if (raw.getCover() != null && raw.getCover().getUrl() != null) {
            String coverUrl = raw.getCover().getUrl()
                    .replace("t_thumb", "t_cover_big")
                    .replace("//", "https://");
            dto.setCoverUrl(coverUrl);
        }

        // Convert release date
        if (raw.getFirstReleaseDate() != null) {
            dto.setReleaseDate(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(raw.getFirstReleaseDate()),
                    ZoneId.systemDefault()
            ));
            dto.setReleaseYear(dto.getReleaseDate().getYear());
        }

        // Extract genres
        if (raw.getGenres() != null) {
            dto.setGenres(raw.getGenres().stream()
                    .map(IGDBRawGame.Genre::getName)
                    .collect(Collectors.toList()));
        }

        // Extract platforms
        if (raw.getPlatforms() != null) {
            dto.setPlatforms(raw.getPlatforms().stream()
                    .map(IGDBRawGame.Platform::getName)
                    .collect(Collectors.toList()));
        }

        // Extract developers and publishers
        if (raw.getInvolvedCompanies() != null) {
            for (IGDBRawGame.InvolvedCompany ic : raw.getInvolvedCompanies()) {
                if (ic.getCompany() != null) {
                    if (Boolean.TRUE.equals(ic.getDeveloper())) {
                        dto.setDeveloper(ic.getCompany().getName());
                    }
                    if (Boolean.TRUE.equals(ic.getPublisher())) {
                        dto.setPublisher(ic.getCompany().getName());
                    }
                }
            }
        }

        // Extract screenshots
        if (raw.getScreenshots() != null) {
            dto.setScreenshotUrls(raw.getScreenshots().stream()
                    .filter(s -> s.getUrl() != null)
                    .map(s -> s.getUrl().replace("t_thumb", "t_screenshot_big").replace("//", "https://"))
                    .collect(Collectors.toList()));
        }

        if (raw.getExternalGames() != null) {
            List<SteamService.SteamGameInfo> steamGameInfos = steamService.getOwnedGames();

            Map<String, SteamService.SteamGameInfo> steamMap = steamGameInfos.stream()
                    .collect(Collectors.toMap(SteamService.SteamGameInfo::getAppId, Function.identity()));

            raw.getExternalGames().forEach(externalGame -> {
                SteamService.SteamGameInfo match = steamMap.get(externalGame.getUid());
                if (match != null) {
                    dto.setSteamAppId(match.getAppId());
                }
            });
        }


        return dto;
    }

    // DTOs for IGDB responses
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TwitchAuthResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("expires_in")
        private Long expiresIn;
        @JsonProperty("token_type")
        private String tokenType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IGDBGameDto {
        private Long igdbId;
        private String name;
        private String summary;
        private String storyline;
        private String coverUrl;
        private LocalDateTime releaseDate;
        private Integer releaseYear;
        private String developer;
        private String publisher;
        private List<String> genres;
        private List<String> platforms;
        private Double rating;
        private Integer ratingCount;
        private Double aggregatedRating;
        private String url;
        private List<String> screenshotUrls;
        private String steamAppId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class IGDBRawGame {
        private Long id;
        private String name;
        private String summary;
        private String storyline;
        private String url;
        private Double rating;
        @JsonProperty("rating_count")
        private Integer ratingCount;
        @JsonProperty("aggregated_rating")
        private Double aggregatedRating;
        @JsonProperty("first_release_date")
        private Long firstReleaseDate;
        private Cover cover;
        private List<Genre> genres;
        private List<Platform> platforms;
        @JsonProperty("involved_companies")
        private List<InvolvedCompany> involvedCompanies;
        private List<Screenshot> screenshots;
        @JsonProperty("external_games")
        private List<ExternalGame> externalGames;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Cover {
            private String url;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Genre {
            private String name;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Platform {
            private String name;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class InvolvedCompany {
            private Company company;
            private Boolean developer;
            private Boolean publisher;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Company {
            private String name;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Screenshot {
            private String url;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ExternalGame {
            private Integer category;
            private String uid;
            private String name;
        }
    }
}
