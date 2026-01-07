package com.infernokun.infernoGames.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infernokun.infernoGames.config.InfernoGamesConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SteamService {

    private static final String STEAM_API_URL = "https://api.steampowered.com";
    private static final String PLAYER_SERVICE = "/IPlayerService";
    private static final String STEAM_USER_SERVICE = "/ISteamUser";
    private static final String STORE_API_URL = "https://store.steampowered.com/api";

    private final InfernoGamesConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Cache of owned games for quick lookup (appId -> SteamGameInfo)
    private final Map<String, SteamGameInfo> ownedGamesCache = new ConcurrentHashMap<>();
    private volatile boolean cacheInitialized = false;
    private volatile long cacheLastUpdated = 0;
    private static final long CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutes

    public SteamService(InfernoGamesConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        // Initialize the owned games cache on startup
        if (isConfigured()) {
            try {
                refreshOwnedGamesCache();
                log.info("Steam service initialized with {} owned games", ownedGamesCache.size());
            } catch (Exception e) {
                log.warn("Failed to initialize Steam owned games cache: {}", e.getMessage());
            }
        } else {
            log.warn("Steam API not configured - missing clientId or clientSecret");
        }
    }

    /**
     * Check if Steam API is properly configured
     */
    public boolean isConfigured() {
        return config.getSteamClientId() != null && !config.getSteamClientId().isEmpty()
                && config.getSteamClientSecret() != null && !config.getSteamClientSecret().isEmpty();
    }

    /**
     * Refresh the owned games cache from Steam API
     */
    @CacheEvict(value = "steamOwnedGames", allEntries = true)
    public void refreshOwnedGamesCache() {
        if (!isConfigured()) {
            log.warn("Cannot refresh Steam cache - API not configured");
            return;
        }

        try {
            String url = String.format(
                    "%s%s/GetOwnedGames/v1?key=%s&steamid=%s&include_appinfo=1&include_played_free_games=1",
                    STEAM_API_URL, PLAYER_SERVICE,
                    config.getSteamClientSecret(),
                    config.getSteamClientId()
            );

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode gamesNode = root.path("response").path("games");

                if (gamesNode.isArray()) {
                    ownedGamesCache.clear();

                    for (JsonNode gameNode : gamesNode) {
                        SteamGameInfo gameInfo = SteamGameInfo.builder()
                                .appId(gameNode.path("appid").asText())
                                .name(gameNode.path("name").asText())
                                .playtimeForever(gameNode.path("playtime_forever").asInt(0))
                                .playtimeWindowsForever(gameNode.path("playtime_windows_forever").asInt(0))
                                .playtimeMacForever(gameNode.path("playtime_mac_forever").asInt(0))
                                .playtimeLinuxForever(gameNode.path("playtime_linux_forever").asInt(0))
                                .playtimeDeckForever(gameNode.path("playtime_deck_forever").asInt(0))
                                .imgIconUrl(gameNode.path("img_icon_url").asText(null))
                                .hasCommunityVisibleStats(gameNode.path("has_community_visible_stats").asBoolean(false))
                                .rtimeLastPlayed(gameNode.path("rtime_last_played").asLong(0))
                                .playtimeDisconnected(gameNode.path("playtime_disconnected").asInt(0))
                                .genres(new ArrayList<>())
                                .inBacklog(false)
                                .build();

                        ownedGamesCache.put(gameInfo.getAppId(), gameInfo);
                    }

                    cacheInitialized = true;
                    cacheLastUpdated = System.currentTimeMillis();
                    log.info("Steam owned games cache refreshed: {} games loaded", ownedGamesCache.size());
                }
            }
        } catch (Exception e) {
            log.error("Failed to refresh Steam owned games cache: {}", e.getMessage());
        }
    }

    /**
     * Check if the cache needs refreshing
     */
    private void ensureCacheValid() {
        if (!cacheInitialized || System.currentTimeMillis() - cacheLastUpdated > CACHE_TTL_MS) {
            refreshOwnedGamesCache();
        }
    }

    /**
     * Get all owned games from Steam
     */
    @Cacheable(value = "steamOwnedGames")
    public List<SteamGameInfo> getOwnedGames() {
        ensureCacheValid();
        return new ArrayList<>(ownedGamesCache.values());
    }

    /**
     * Check if a game is owned on Steam by app ID
     * Returns the game info if owned, empty optional if not
     */
    public Optional<SteamGameInfo> checkOwnership(String appId) {
        if (appId == null || appId.isEmpty()) {
            return Optional.empty();
        }

        ensureCacheValid();
        return Optional.ofNullable(ownedGamesCache.get(appId));
    }

    /**
     * Check if a game is owned on Steam (simple boolean check)
     */
    public boolean isGameOwned(String appId) {
        return checkOwnership(appId).isPresent();
    }

    /**
     * Get Steam game info by app ID (from cache or API)
     */
    public Optional<SteamGameInfo> getGameInfo(String appId) {
        // First check the owned games cache
        Optional<SteamGameInfo> owned = checkOwnership(appId);
        if (owned.isPresent()) {
            return owned;
        }

        // If not owned, we can still get basic info from the Store API
        return getGameInfoFromStore(appId);
    }

    /**
     * Get game info from Steam Store API (for games not owned)
     */
    private Optional<SteamGameInfo> getGameInfoFromStore(String appId) {
        try {
            String url = String.format("%s/appdetails?appids=%s", STORE_API_URL, appId);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode appData = root.path(appId);

                if (appData.path("success").asBoolean(false)) {
                    JsonNode data = appData.path("data");
                    return Optional.of(SteamGameInfo.builder()
                            .appId(appId)
                            .name(data.path("name").asText())
                            .imgIconUrl(null) // Store API doesn't provide icon URL in same format
                            .playtimeForever(0)
                            .genres(new ArrayList<>())
                            .inBacklog(false)
                            .build());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get store info for app {}: {}", appId, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Get playtime data for a specific game
     */
    public Optional<SteamPlaytimeInfo> getPlaytimeInfo(String appId) {
        return checkOwnership(appId).map(game -> SteamPlaytimeInfo.builder()
                .appId(game.getAppId())
                .playtimeForeverMinutes(game.getPlaytimeForever())
                .playtimeForeverHours(game.getPlaytimeForever() / 60.0)
                .playtimeWindowsMinutes(game.getPlaytimeWindowsForever())
                .playtimeMacMinutes(game.getPlaytimeMacForever())
                .playtimeLinuxMinutes(game.getPlaytimeLinuxForever())
                .playtimeDeckMinutes(game.getPlaytimeDeckForever())
                .playtimeDisconnectedMinutes(game.getPlaytimeDisconnected())
                .lastPlayed(game.getRtimeLastPlayed() > 0 ?
                        LocalDateTime.ofInstant(Instant.ofEpochSecond(game.getRtimeLastPlayed()), ZoneId.systemDefault()) :
                        null)
                .build());
    }

    /**
     * Get recently played games from Steam
     */
    public List<SteamGameInfo> getRecentlyPlayedGames(int count) {
        if (!isConfigured()) {
            return Collections.emptyList();
        }

        try {
            String url = String.format(
                    "%s%s/GetRecentlyPlayedGames/v1?key=%s&steamid=%s&count=%d",
                    STEAM_API_URL, PLAYER_SERVICE,
                    config.getSteamClientSecret(),
                    config.getSteamClientId(),
                    count
            );

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode gamesNode = root.path("response").path("games");

                if (gamesNode.isArray()) {
                    List<SteamGameInfo> games = new ArrayList<>();
                    for (JsonNode gameNode : gamesNode) {
                        games.add(SteamGameInfo.builder()
                                .appId(gameNode.path("appid").asText())
                                .name(gameNode.path("name").asText())
                                .playtimeForever(gameNode.path("playtime_forever").asInt(0))
                                .playtimeWindowsForever(gameNode.path("playtime_windows_forever").asInt(0))
                                .playtimeLinuxForever(gameNode.path("playtime_linux_forever").asInt(0))
                                .playtimeDeckForever(gameNode.path("playtime_deck_forever").asInt(0))
                                .imgIconUrl(gameNode.path("img_icon_url").asText(null))
                                .build());
                    }
                    return games;
                }
            }
        } catch (Exception e) {
            log.error("Failed to get recently played games: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * Search owned games by name
     */
    public List<SteamGameInfo> searchOwnedGames(String query) {
        ensureCacheValid();
        String lowerQuery = query.toLowerCase();
        return ownedGamesCache.values().stream()
                .filter(game -> game.getName() != null && game.getName().toLowerCase().contains(lowerQuery))
                .sorted(Comparator.comparing(SteamGameInfo::getName))
                .collect(Collectors.toList());
    }

    /**
     * Get games with playtime, sorted by most played
     */
    public List<SteamGameInfo> getMostPlayedGames(int limit) {
        ensureCacheValid();
        return ownedGamesCache.values().stream()
                .filter(game -> game.getPlaytimeForever() > 0)
                .sorted(Comparator.comparingInt(SteamGameInfo::getPlaytimeForever).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get total Steam library stats
     */
    public SteamLibraryStats getLibraryStats() {
        ensureCacheValid();

        int totalGames = ownedGamesCache.size();
        int playedGames = (int) ownedGamesCache.values().stream()
                .filter(g -> g.getPlaytimeForever() > 0)
                .count();
        int totalPlaytimeMinutes = ownedGamesCache.values().stream()
                .mapToInt(SteamGameInfo::getPlaytimeForever)
                .sum();
        int deckPlaytimeMinutes = ownedGamesCache.values().stream()
                .mapToInt(SteamGameInfo::getPlaytimeDeckForever)
                .sum();
        int windowsPlaytimeMinutes = ownedGamesCache.values().stream()
                .mapToInt(SteamGameInfo::getPlaytimeWindowsForever)
                .sum();
        int linuxPlaytimeMinutes = ownedGamesCache.values().stream()
                .mapToInt(SteamGameInfo::getPlaytimeLinuxForever)
                .sum();

        return SteamLibraryStats.builder()
                .totalGames(totalGames)
                .playedGames(playedGames)
                .unplayedGames(totalGames - playedGames)
                .totalPlaytimeMinutes(totalPlaytimeMinutes)
                .totalPlaytimeHours(totalPlaytimeMinutes / 60.0)
                .deckPlaytimeMinutes(deckPlaytimeMinutes)
                .deckPlaytimeHours(deckPlaytimeMinutes / 60.0)
                .windowsPlaytimeMinutes(windowsPlaytimeMinutes)
                .windowsPlaytimeHours(windowsPlaytimeMinutes / 60.0)
                .linuxPlaytimeMinutes(linuxPlaytimeMinutes)
                .linuxPlaytimeHours(linuxPlaytimeMinutes / 60.0)
                .playedPercentage(totalGames > 0 ? (playedGames * 100.0 / totalGames) : 0)
                .build();
    }

    /**
     * Build icon URL for a Steam game
     */
    public String buildIconUrl(String appId, String iconHash) {
        if (iconHash == null || iconHash.isEmpty()) {
            return null;
        }
        return String.format("https://media.steampowered.com/steamcommunity/public/images/apps/%s/%s.jpg",
                appId, iconHash);
    }

    /**
     * Build header image URL for a Steam game
     */
    public String buildHeaderImageUrl(String appId) {
        return String.format("https://cdn.cloudflare.steamstatic.com/steam/apps/%s/header.jpg", appId);
    }

    /**
     * Get Steam user profile information using ISteamUser API
     */
    @Cacheable(value = "steamUserProfile", key = "#root.method.name")
    public Optional<SteamUserProfile> getUserProfile() {
        if (!isConfigured()) {
            log.warn("Cannot get Steam user profile - API not configured");
            return Optional.empty();
        }

        try {
            String url = String.format(
                    "%s%s/GetPlayerSummaries/v2?key=%s&steamids=%s",
                    STEAM_API_URL, STEAM_USER_SERVICE,
                    config.getSteamClientSecret(),
                    config.getSteamClientId()
            );

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode playersNode = root.path("response").path("players");

                if (playersNode.isArray() && !playersNode.isEmpty()) {
                    JsonNode playerNode = playersNode.get(0);

                    SteamUserProfile profile = SteamUserProfile.builder()
                            .steamId(playerNode.path("steamid").asText())
                            .personaName(playerNode.path("personaname").asText())
                            .profileUrl(playerNode.path("profileurl").asText())
                            .avatar(playerNode.path("avatar").asText())
                            .avatarMedium(playerNode.path("avatarmedium").asText())
                            .avatarFull(playerNode.path("avatarfull").asText())
                            .avatarHash(playerNode.path("avatarhash").asText())
                            .personaState(playerNode.path("personastate").asInt(0))
                            .communityVisibilityState(playerNode.path("communityvisibilitystate").asInt(0))
                            .profileState(playerNode.path("profilestate").asInt(0))
                            .lastLogoff(playerNode.path("lastlogoff").asLong(0))
                            .realName(playerNode.path("realname").asText(null))
                            .countryCode(playerNode.path("loccountrycode").asText(null))
                            .stateCode(playerNode.path("locstatecode").asText(null))
                            .cityId(playerNode.path("loccityid").asInt(0))
                            .timeCreated(playerNode.path("timecreated").asLong(0))
                            .build();

                    log.info("Steam user profile loaded: {}", profile.getPersonaName());
                    return Optional.of(profile);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get Steam user profile: {}", e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Clear user profile cache
     */
    @CacheEvict(value = "steamUserProfile", allEntries = true)
    public void clearUserProfileCache() {
        log.info("Steam user profile cache cleared");
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SteamUserProfile {
        private String steamId;
        private String personaName;
        private String profileUrl;
        private String avatar;           // 32x32
        private String avatarMedium;     // 64x64
        private String avatarFull;       // 184x184
        private String avatarHash;
        private int personaState;        // 0=Offline, 1=Online, 2=Busy, 3=Away, 4=Snooze, 5=Looking to trade, 6=Looking to play
        private int communityVisibilityState;  // 1=Private, 3=Public
        private int profileState;        // 0=Not configured, 1=Configured
        private long lastLogoff;         // Unix timestamp
        private String realName;
        private String countryCode;
        private String stateCode;
        private int cityId;
        private long timeCreated;        // Unix timestamp of account creation

        public String getPersonaStateString() {
            return switch (personaState) {
                case 1 -> "Online";
                case 2 -> "Busy";
                case 3 -> "Away";
                case 4 -> "Snooze";
                case 5 -> "Looking to Trade";
                case 6 -> "Looking to Play";
                default -> "Offline";
            };
        }

        public LocalDateTime getLastLogoffDateTime() {
            if (lastLogoff <= 0) return null;
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(lastLogoff), ZoneId.systemDefault());
        }

        public LocalDateTime getAccountCreatedDateTime() {
            if (timeCreated <= 0) return null;
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(timeCreated), ZoneId.systemDefault());
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SteamGameInfo {
        private String appId;
        private String name;
        private int playtimeForever;          // Total playtime in minutes
        private int playtimeWindowsForever;   // Windows playtime in minutes
        private int playtimeMacForever;       // Mac playtime in minutes
        private int playtimeLinuxForever;     // Linux playtime in minutes
        private int playtimeDeckForever;      // Steam Deck playtime in minutes
        private String imgIconUrl;
        private boolean hasCommunityVisibleStats;
        private long rtimeLastPlayed;         // Unix timestamp of last played
        private int playtimeDisconnected;     // Offline playtime in minutes

        // Enrichment fields (populated when cross-referenced with backlog)
        @Builder.Default
        private List<String> genres = new ArrayList<>();
        private boolean inBacklog;
        private Long backlogGameId;

        public double getPlaytimeForeverHours() {
            return playtimeForever / 60.0;
        }

        public LocalDateTime getLastPlayedDateTime() {
            if (rtimeLastPlayed <= 0) return null;
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(rtimeLastPlayed), ZoneId.systemDefault());
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SteamPlaytimeInfo {
        private String appId;
        private int playtimeForeverMinutes;
        private double playtimeForeverHours;
        private int playtimeWindowsMinutes;
        private int playtimeMacMinutes;
        private int playtimeLinuxMinutes;
        private int playtimeDeckMinutes;
        private int playtimeDisconnectedMinutes;
        private LocalDateTime lastPlayed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SteamLibraryStats {
        private int totalGames;
        private int playedGames;
        private int unplayedGames;
        private int totalPlaytimeMinutes;
        private double totalPlaytimeHours;
        private int deckPlaytimeMinutes;
        private double deckPlaytimeHours;
        private int windowsPlaytimeMinutes;
        private double windowsPlaytimeHours;
        private int linuxPlaytimeMinutes;
        private double linuxPlaytimeHours;
        private double playedPercentage;
    }
}