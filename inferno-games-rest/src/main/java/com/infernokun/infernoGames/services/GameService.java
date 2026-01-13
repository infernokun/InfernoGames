package com.infernokun.infernoGames.services;

import com.infernokun.infernoGames.models.Game;
import com.infernokun.infernoGames.models.dto.GameRequest;
import com.infernokun.infernoGames.models.enums.GamePlatform;
import com.infernokun.infernoGames.models.enums.GameStatus;
import com.infernokun.infernoGames.repositories.GameRepository;
import com.infernokun.infernoGames.services.IGDBService.IGDBGameDto;
import com.infernokun.infernoGames.services.SteamService.SteamGameInfo;
import com.infernokun.infernoGames.services.SteamService.SteamUserProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final IGDBService igdbService;
    private final SteamService steamService;

    // ─── CRUD Operations ────────────────────────────────────────────────────────

    @Cacheable(value = "games")
    public List<Game> getAllGames() {
        return gameRepository.findAllByOrderByTitleAsc();
    }

    @Cacheable(value = "game", key = "#id")
    public Game getGameById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Game not found with id: " + id));
    }

    public Optional<Game> getGameByIgdbId(Long igdbId) {
        return gameRepository.findByIgdbId(igdbId);
    }

    @CacheEvict(value = {"games", "gameStats"}, allEntries = true)
    public Game createGame(GameRequest request) {
        // Check Steam ownership and get playtime data if Steam App ID is provided
        SteamGameInfo steamInfo = null;
        GamePlatform platform = request.getPlatform();

        if (request.getSteamAppId() != null && !request.getSteamAppId().isEmpty()) {
            Optional<SteamGameInfo> steamOwnership = steamService.checkOwnership(request.getSteamAppId());
            if (steamOwnership.isPresent()) {
                steamInfo = steamOwnership.get();
                // Auto-set platform to PC if game is owned on Steam and no platform specified
                if (platform == null) {
                    platform = GamePlatform.PC;
                    log.info("Auto-setting platform to PC for Steam-owned game: {}", request.getTitle());
                }
            } else {
                log.debug("Game with Steam App ID {} not found in user's Steam library", request.getSteamAppId());
            }
        }

        Game game = Game.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .developer(request.getDeveloper())
                .publisher(request.getPublisher())
                .releaseYear(request.getReleaseYear())
                .genre(request.getGenre())
                .genres(request.getGenres() != null ? request.getGenres() : new ArrayList<>())
                .coverImageUrl(request.getCoverImageUrl())
                .screenshotUrls(request.getScreenshotUrls() != null ? request.getScreenshotUrls() : new ArrayList<>())
                .platform(platform)
                .platforms(request.getPlatforms() != null ? request.getPlatforms() : new ArrayList<>())
                .status(request.getStatus() != null ? request.getStatus() : GameStatus.NOT_STARTED)
                .rating(request.getRating())
                .playtimeHours(request.getPlaytimeHours() != null ? request.getPlaytimeHours() : 0.0)
                .completionPercentage(request.getCompletionPercentage() != null ? request.getCompletionPercentage() : 0)
                .startedAt(request.getStartedAt())
                .completedAt(request.getCompletedAt())
                .notes(request.getNotes())
                .favorite(request.getFavorite() != null ? request.getFavorite() : false)
                .achievements(request.getAchievements() != null ? request.getAchievements() : 0)
                .totalAchievements(request.getTotalAchievements() != null ? request.getTotalAchievements() : 0)
                .igdbId(request.getIgdbId())
                .steamAppId(request.getSteamAppId())
                .build();

        // Populate Steam data if available
        if (steamInfo != null) {
            populateFromSteam(game, steamInfo);
        }

        log.info("Creating new game: {}", game.getTitle());
        return gameRepository.save(game);
    }

    /**
     * Populate game fields from Steam data
     */
    private void populateFromSteam(Game game, SteamGameInfo steamInfo) {
        // Set playtime from Steam (convert minutes to hours)
        double playtimeHours = steamInfo.getPlaytimeForever() / 60.0;
        if (game.getPlaytimeHours() == null || game.getPlaytimeHours() == 0.0) {
            game.setPlaytimeHours(playtimeHours);
        }

        // Set platform-specific playtimes
        game.setSteamPlaytimeWindowsMinutes(steamInfo.getPlaytimeWindowsForever());
        game.setSteamPlaytimeLinuxMinutes(steamInfo.getPlaytimeLinuxForever());
        game.setSteamPlaytimeMacMinutes(steamInfo.getPlaytimeMacForever());
        game.setSteamPlaytimeDeckMinutes(steamInfo.getPlaytimeDeckForever());

        // Set last played timestamp
        if (steamInfo.getRtimeLastPlayed() > 0) {
            game.setSteamLastPlayed(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(steamInfo.getRtimeLastPlayed()),
                    ZoneId.systemDefault()
            ));
        }

        game.setSteamLastSynced(LocalDateTime.now());

        log.debug("Populated Steam data for '{}': {} hours total, {} deck, {} windows",
                game.getTitle(),
                String.format("%.1f", playtimeHours),
                steamInfo.getPlaytimeDeckForever(),
                steamInfo.getPlaytimeWindowsForever());
    }

    @CacheEvict(value = {"games", "game", "gameStats"}, allEntries = true)
    public Game updateGame(Long id, GameRequest request) {
        Game game = getGameById(id);

        if (request.getTitle() != null) game.setTitle(request.getTitle());
        if (request.getDescription() != null) game.setDescription(request.getDescription());
        if (request.getDeveloper() != null) game.setDeveloper(request.getDeveloper());
        if (request.getPublisher() != null) game.setPublisher(request.getPublisher());
        if (request.getReleaseYear() != null) game.setReleaseYear(request.getReleaseYear());
        if (request.getGenre() != null) game.setGenre(request.getGenre());
        if (request.getGenres() != null) game.setGenres(request.getGenres());
        if (request.getCoverImageUrl() != null) game.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getScreenshotUrls() != null) game.setScreenshotUrls(request.getScreenshotUrls());
        if (request.getPlatform() != null) game.setPlatform(request.getPlatform());
        if (request.getPlatforms() != null) game.setPlatforms(request.getPlatforms());
        if (request.getRating() != null) game.setRating(request.getRating());
        if (request.getPlaytimeHours() != null) game.setPlaytimeHours(request.getPlaytimeHours());
        if (request.getCompletionPercentage() != null) game.setCompletionPercentage(request.getCompletionPercentage());
        if (request.getStartedAt() != null) game.setStartedAt(request.getStartedAt());
        if (request.getCompletedAt() != null) game.setCompletedAt(request.getCompletedAt());
        if (request.getNotes() != null) game.setNotes(request.getNotes());
        if (request.getFavorite() != null) game.setFavorite(request.getFavorite());
        if (request.getAchievements() != null) game.setAchievements(request.getAchievements());
        if (request.getTotalAchievements() != null) game.setTotalAchievements(request.getTotalAchievements());

        // Handle status change with timestamp updates
        if (request.getStatus() != null && request.getStatus() != game.getStatus()) {
            game.updateStatus(request.getStatus());
        }

        log.info("Updating game: {} (ID: {})", game.getTitle(), id);
        return gameRepository.save(game);
    }

    @CacheEvict(value = {"games", "game", "gameStats"}, allEntries = true)
    public void deleteGame(Long id) {
        Game game = getGameById(id);
        log.info("Deleting game: {} (ID: {})", game.getTitle(), id);
        gameRepository.delete(game);
    }

    // ─── Status Operations ──────────────────────────────────────────────────────

    @CacheEvict(value = {"games", "game", "gameStats"}, allEntries = true)
    public Game updateGameStatus(Long id, GameStatus status) {
        Game game = getGameById(id);
        game.updateStatus(status);
        log.info("Updated status for game '{}' to {}", game.getTitle(), status);
        return gameRepository.save(game);
    }

    @CacheEvict(value = {"games", "game"}, allEntries = true)
    public Game toggleFavorite(Long id) {
        Game game = getGameById(id);
        game.setFavorite(!game.getFavorite());
        log.info("Toggled favorite for game '{}': {}", game.getTitle(), game.getFavorite());
        return gameRepository.save(game);
    }

    // ─── Query Operations ───────────────────────────────────────────────────────

    public List<Game> searchGames(String query) {
        return gameRepository.findByTitleContainingIgnoreCase(query);
    }

    public List<Game> getGamesByStatus(GameStatus status) {
        return gameRepository.findByStatus(status);
    }

    public List<Game> getGamesByPlatform(GamePlatform platform) {
        return gameRepository.findByPlatform(platform);
    }

    public List<Game> getFavoriteGames() {
        return gameRepository.findByFavoriteTrue();
    }

    public List<Game> advancedSearch(String title, GameStatus status, GamePlatform platform, String genre) {
        return gameRepository.searchGames(title, status, platform, genre);
    }

    public List<Game> getRecentlyAddedGames() {
        return gameRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public List<Game> getRecentlyCompletedGames() {
        return gameRepository.findByStatusOrderByCompletedAtDesc(GameStatus.COMPLETED);
    }

    // ─── Statistics ─────────────────────────────────────────────────────────────

    @Cacheable(value = "gameStats")
    public Map<String, Object> getGameStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalGames = gameRepository.count();
        long completedGames = gameRepository.countByStatus(GameStatus.COMPLETED);
        long inProgressGames = gameRepository.countByStatus(GameStatus.IN_PROGRESS);
        long notStartedGames = gameRepository.countByStatus(GameStatus.NOT_STARTED);
        long onHoldGames = gameRepository.countByStatus(GameStatus.ON_HOLD);
        long droppedGames = gameRepository.countByStatus(GameStatus.DROPPED);
        long dlcGames = gameRepository.countByStatus(GameStatus.DLC);
        long favoriteGames = gameRepository.countByFavoriteTrue();
        Double totalPlaytime = gameRepository.getTotalPlaytime();
        Double averageRating = gameRepository.getAverageRating();

        stats.put("totalGames", totalGames);
        stats.put("completedGames", completedGames);
        stats.put("inProgressGames", inProgressGames);
        stats.put("notStartedGames", notStartedGames);
        stats.put("onHoldGames", onHoldGames);
        stats.put("droppedGames", droppedGames);
        stats.put("dlcGames", dlcGames);
        stats.put("favoriteGames", favoriteGames);
        stats.put("totalPlaytime", totalPlaytime != null ? totalPlaytime : 0.0);
        stats.put("averageRating", averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0);
        stats.put("completionRate", totalGames > 0 ? Math.round((completedGames * 100.0) / totalGames) : 0);

        // Platform breakdown
        Map<String, Long> platformStats = new HashMap<>();
        for (GamePlatform platform : GamePlatform.values()) {
            long count = gameRepository.findByPlatform(platform).size();
            if (count > 0) {
                platformStats.put(platform.name(), count);
            }
        }
        stats.put("platformBreakdown", platformStats);

        // Genre breakdown
        Map<String, Long> genreStats = getAllGames().stream()
                .filter(g -> g.getGenre() != null && !g.getGenre().isEmpty())
                .collect(Collectors.groupingBy(Game::getGenre, Collectors.counting()));
        stats.put("genreBreakdown", genreStats);

        return stats;
    }

    // ─── IGDB Integration ───────────────────────────────────────────────────────

    public List<IGDBGameDto> searchIGDB(String query) {
        return igdbService.searchGames(query);
    }

    public Optional<IGDBGameDto> getIGDBGameById(Long igdbId) {
        return igdbService.getGameById(igdbId);
    }

    public List<IGDBGameDto> getPopularIGDBGames(int limit) {
        return igdbService.getPopularGames(limit);
    }

    public List<IGDBGameDto> getRecentIGDBReleases(int limit) {
        return igdbService.getRecentReleases(limit);
    }

    public List<IGDBGameDto> getUpcomingIGDBGames(int limit) {
        return igdbService.getUpcomingGames(limit);
    }

    @CacheEvict(value = {"games", "gameStats"}, allEntries = true)
    public Game createGameFromIGDB(Long igdbId) {
        // Check if game already exists
        Optional<Game> existing = gameRepository.findByIgdbId(igdbId);
        if (existing.isPresent()) {
            log.info("Game from IGDB {} already exists: {}", igdbId, existing.get().getTitle());
            return existing.get();
        }

        // Fetch from IGDB
        Optional<IGDBGameDto> igdbGame = igdbService.getGameById(igdbId);
        if (igdbGame.isEmpty()) {
            throw new IllegalArgumentException("Game not found in IGDB with id: " + igdbId);
        }

        IGDBGameDto dto = igdbGame.get();

        // Check Steam ownership and get playtime data
        SteamGameInfo steamInfo = null;
        GamePlatform platform = null;

        if (dto.getSteamAppId() != null && !dto.getSteamAppId().isEmpty()) {
            Optional<SteamGameInfo> steamOwnership = steamService.checkOwnership(dto.getSteamAppId());
            if (steamOwnership.isPresent()) {
                steamInfo = steamOwnership.get();
                platform = GamePlatform.PC;
                log.info("Steam ownership verified for IGDB game '{}' (Steam App ID: {})",
                        dto.getName(), dto.getSteamAppId());
            }
        }

        Game game = Game.builder()
                .title(dto.getName())
                .description(dto.getSummary())
                .developer(dto.getDeveloper())
                .publisher(dto.getPublisher())
                .releaseYear(dto.getReleaseYear())
                .releaseDate(dto.getReleaseDate())
                .genre(dto.getGenres() != null && !dto.getGenres().isEmpty() ? dto.getGenres().getFirst() : null)
                .genres(dto.getGenres() != null ? dto.getGenres() : new ArrayList<>())
                .coverImageUrl(dto.getCoverUrl())
                .screenshotUrls(dto.getScreenshotUrls() != null ? dto.getScreenshotUrls() : new ArrayList<>())
                .platform(platform)
                .status(GameStatus.NOT_STARTED)
                .igdbId(dto.getIgdbId())
                .igdbUrl(dto.getUrl())
                .igdbRating(dto.getRating())
                .igdbRatingCount(dto.getRatingCount())
                .favorite(false)
                .playtimeHours(0.0)
                .completionPercentage(0)
                .achievements(0)
                .totalAchievements(0)
                .steamAppId(dto.getSteamAppId())
                .build();

        // Populate Steam data if available
        if (steamInfo != null) {
            populateFromSteam(game, steamInfo);
        }

        log.info("Creating game from IGDB: {} (IGDB ID: {})", game.getTitle(), igdbId);
        return gameRepository.save(game);
    }

    @CacheEvict(value = {"games", "game"}, allEntries = true)
    public Game refreshFromIGDB(Long gameId) {
        Game game = getGameById(gameId);

        if (game.getIgdbId() == null) {
            throw new IllegalArgumentException("Game has no IGDB ID associated");
        }

        Optional<IGDBGameDto> igdbGame = igdbService.getGameById(game.getIgdbId());
        if (igdbGame.isEmpty()) {
            throw new IllegalArgumentException("Game not found in IGDB");
        }

        IGDBGameDto dto = igdbGame.get();

        // Update fields from IGDB (preserve user data like status, playtime, etc.)
        game.setDescription(dto.getSummary());
        game.setDeveloper(dto.getDeveloper());
        game.setPublisher(dto.getPublisher());
        game.setReleaseYear(dto.getReleaseYear());
        game.setReleaseDate(dto.getReleaseDate());
        game.setGenre(dto.getGenres() != null && !dto.getGenres().isEmpty() ? dto.getGenres().getFirst() : null);
        game.setGenres(dto.getGenres() != null ? dto.getGenres() : game.getGenres());
        game.setCoverImageUrl(dto.getCoverUrl());
        game.setScreenshotUrls(dto.getScreenshotUrls() != null ? dto.getScreenshotUrls() : game.getScreenshotUrls());
        game.setIgdbUrl(dto.getUrl());
        game.setIgdbRating(dto.getRating());
        game.setIgdbRatingCount(dto.getRatingCount());

        log.info("Refreshed game from IGDB: {} (ID: {})", game.getTitle(), gameId);
        return gameRepository.save(game);
    }

    /**
     * Batch refresh all games from IGDB to populate missing genres
     * This is useful for existing games that were imported before genres were tracked
     */
    @CacheEvict(value = {"games", "game", "gameStats"}, allEntries = true)
    public Map<String, Object> refreshAllGenresFromIGDB() {
        List<Game> gamesWithIgdbId = gameRepository.findAll().stream()
                .filter(g -> g.getIgdbId() != null)
                .filter(g -> g.getGenres() == null || g.getGenres().isEmpty())
                .toList();

        int successCount = 0;
        int failCount = 0;
        List<String> failedGames = new ArrayList<>();

        for (Game game : gamesWithIgdbId) {
            try {
                Optional<IGDBGameDto> igdbGame = igdbService.getGameById(game.getIgdbId());
                if (igdbGame.isPresent() && igdbGame.get().getGenres() != null) {
                    IGDBGameDto dto = igdbGame.get();
                    game.setGenres(dto.getGenres());
                    if (game.getGenre() == null && !dto.getGenres().isEmpty()) {
                        game.setGenre(dto.getGenres().getFirst());
                    }
                    gameRepository.save(game);
                    successCount++;
                    log.info("Updated genres for: {} -> {}", game.getTitle(), dto.getGenres());
                } else {
                    failCount++;
                    failedGames.add(game.getTitle());
                }
                // Add a small delay to avoid rate limiting
                Thread.sleep(250);
            } catch (Exception e) {
                failCount++;
                failedGames.add(game.getTitle() + " (" + e.getMessage() + ")");
                log.warn("Failed to refresh genres for {}: {}", game.getTitle(), e.getMessage());
            }
        }

        log.info("Genre refresh completed: {} succeeded, {} failed", successCount, failCount);

        Map<String, Object> result = new HashMap<>();
        result.put("totalProcessed", gamesWithIgdbId.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failedGames", failedGames);
        return result;
    }

    // ─── Cache Management ───────────────────────────────────────────────────────

    @CacheEvict(value = {"games", "game", "gameStats"}, allEntries = true)
    public void clearAllCaches() {
        log.info("Cleared all game caches");
    }

    // ─── Steam Integration ───────────────────────────────────────────────────────

    /**
     * Check if a Steam App ID corresponds to a game owned by the user
     */
    public boolean checkSteamOwnership(String steamAppId) {
        return steamService.isGameOwned(steamAppId);
    }

    /**
     * Get Steam game info by app ID
     */
    public Optional<SteamGameInfo> getSteamGameInfo(String steamAppId) {
        return steamService.getGameInfo(steamAppId);
    }

    /**
     * Get all Steam owned games
     */
    public List<SteamGameInfo> getSteamOwnedGames() {
        return steamService.getOwnedGames();
    }

    /**
     * Get Steam library with genres - returns immediately with available data
     * Background scheduler handles IGDB enrichment for non-backlog games
     */
    public List<SteamGameInfo> getSteamLibraryWithGenres() {
        List<SteamGameInfo> steamGames = new ArrayList<>(steamService.getOwnedGames());

        // Create a map of Steam App ID to backlog games for quick lookup
        Map<String, Game> steamAppIdToGame = gameRepository.findAll().stream()
                .filter(g -> g.getSteamAppId() != null && !g.getSteamAppId().isEmpty())
                .collect(Collectors.toMap(
                        Game::getSteamAppId,
                        g -> g,
                        (existing, replacement) -> existing
                ));

        // Get cached IGDB genres
        Map<String, List<String>> cachedGenres = getCachedSteamGenres();

        for (SteamGameInfo steamGame : steamGames) {
            Game backlogGame = steamAppIdToGame.get(steamGame.getAppId());

            if (backlogGame != null) {
                // Game is in backlog - use backlog data
                steamGame.setInBacklog(true);
                steamGame.setBacklogGameId(backlogGame.getId());
                if (backlogGame.getGenres() != null && !backlogGame.getGenres().isEmpty()) {
                    steamGame.setGenres(new ArrayList<>(backlogGame.getGenres()));
                }
            } else {
                // Check if we have cached IGDB genres
                List<String> cached = cachedGenres.get(steamGame.getAppId());
                if (cached != null && !cached.isEmpty()) {
                    steamGame.setGenres(new ArrayList<>(cached));
                }
            }
        }

        return steamGames;
    }

    // In-memory cache for IGDB genre lookups (appId -> genres)
    private final Map<String, List<String>> igdbGenreCache = new ConcurrentHashMap<>();
    @Getter
    private volatile boolean enrichmentInProgress = false;

    public Map<String, List<String>> getCachedSteamGenres() {
        return new HashMap<>(igdbGenreCache);
    }

    public int getCachedGenreCount() {
        return igdbGenreCache.size();
    }

    /**
     * Background task to enrich Steam games with IGDB genres
     * Called by scheduler - processes games gradually to avoid rate limits
     */
    public void enrichSteamLibraryGenresInBackground() {
        if (enrichmentInProgress) {
            log.debug("Genre enrichment already in progress, skipping");
            return;
        }

        enrichmentInProgress = true;

        try {
            List<SteamGameInfo> steamGames = steamService.getOwnedGames();

            // Get games already in backlog (they have genres)
            Set<String> backlogAppIds = gameRepository.findAll().stream()
                    .filter(g -> g.getSteamAppId() != null && !g.getSteamAppId().isEmpty())
                    .map(Game::getSteamAppId)
                    .collect(Collectors.toSet());

            // Find games that need IGDB lookup (not in backlog, not already cached)
            List<SteamGameInfo> gamesNeedingLookup = steamGames.stream()
                    .filter(g -> !backlogAppIds.contains(g.getAppId()))
                    .filter(g -> !igdbGenreCache.containsKey(g.getAppId()))
                    .toList();

            if (gamesNeedingLookup.isEmpty()) {
                log.debug("No games need IGDB genre enrichment");
                return;
            }

            log.info("Starting background IGDB genre enrichment for {} games", gamesNeedingLookup.size());

            int processed = 0;
            int enriched = 0;

            for (SteamGameInfo game : gamesNeedingLookup) {
                // Rate limit: 3 requests per second
                if (processed > 0 && processed % 3 == 0) {
                    Thread.sleep(1000);
                }

                try {
                    List<IGDBGameDto> igdbResults = igdbService.searchGames(game.getName());
                    processed++;

                    if (!igdbResults.isEmpty()) {
                        IGDBGameDto match = igdbResults.stream()
                                .filter(g -> game.getAppId().equals(g.getSteamAppId()))
                                .findFirst()
                                .orElseGet(() -> igdbResults.stream()
                                        .filter(g -> game.getName().equalsIgnoreCase(g.getName()))
                                        .findFirst()
                                        .orElse(igdbResults.getFirst()));

                        if (match.getGenres() != null && !match.getGenres().isEmpty()) {
                            igdbGenreCache.put(game.getAppId(), new ArrayList<>(match.getGenres()));
                            enriched++;
                        } else {
                            // Cache empty list to avoid re-fetching
                            igdbGenreCache.put(game.getAppId(), new ArrayList<>());
                        }
                    } else {
                        igdbGenreCache.put(game.getAppId(), new ArrayList<>());
                    }
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("authentication failed")) {
                        log.warn("IGDB authentication failed - stopping enrichment");
                        break;
                    } else if (msg.contains("429") || msg.contains("Too Many Requests")) {
                        log.warn("IGDB rate limit hit - pausing enrichment");
                        Thread.sleep(5000);
                    } else {
                        log.debug("Failed to fetch IGDB data for {}: {}", game.getName(), msg);
                        igdbGenreCache.put(game.getAppId(), new ArrayList<>());
                    }
                }
            }

            log.info("Background genre enrichment complete: {}/{} games enriched", enriched, processed);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Genre enrichment interrupted");
        } catch (Exception e) {
            log.error("Error during genre enrichment: {}", e.getMessage());
        } finally {
            enrichmentInProgress = false;
        }
    }

    /**
     * Clear the IGDB genre cache
     */
    public void clearIgdbGenreCache() {
        igdbGenreCache.clear();
        log.info("Cleared IGDB genre cache");
    }

    /**
     * Search owned Steam games by name
     */
    public List<SteamGameInfo> searchSteamGames(String query) {
        return steamService.searchOwnedGames(query);
    }

    /**
     * Get recently played Steam games
     */
    public List<SteamGameInfo> getRecentlyPlayedSteamGames(int count) {
        return steamService.getRecentlyPlayedGames(count);
    }

    /**
     * Get most played Steam games
     */
    public List<SteamGameInfo> getMostPlayedSteamGames(int limit) {
        return steamService.getMostPlayedGames(limit);
    }

    /**
     * Get Steam library statistics
     */
    public SteamService.SteamLibraryStats getSteamLibraryStats() {
        return steamService.getLibraryStats();
    }

    /**
     * Refresh Steam owned games cache
     */
    public void refreshSteamCache() {
        steamService.refreshOwnedGamesCache();
    }

    /**
     * Sync a single game's Steam data
     */
    @CacheEvict(value = {"games", "game"}, allEntries = true)
    public Game syncGameSteamData(Long gameId) {
        Game game = getGameById(gameId);

        if (game.getSteamAppId() == null || game.getSteamAppId().isEmpty()) {
            throw new IllegalArgumentException("Game has no Steam App ID");
        }

        Optional<SteamGameInfo> steamInfo = steamService.checkOwnership(game.getSteamAppId());
        if (steamInfo.isEmpty()) {
            throw new IllegalArgumentException("Game not found in Steam library");
        }

        populateFromSteam(game, steamInfo.get());
        log.info("Synced Steam data for game '{}' (ID: {})", game.getTitle(), gameId);
        return gameRepository.save(game);
    }

    /**
     * Check Steam API configuration status
     */
    public boolean isSteamConfigured() {
        return steamService.isConfigured();
    }

    /**
     * Get Steam user profile
     */
    public Optional<SteamUserProfile> getSteamUserProfile() {
        return steamService.getUserProfile();
    }

    /**
     * Migrate existing games with Steam App IDs to populate Steam data
     */
    @CacheEvict(value = {"games", "game", "gameStats"}, allEntries = true)
    public int migrateExistingSteamData() {
        if (!steamService.isConfigured()) {
            log.warn("Steam API not configured - cannot migrate");
            return 0;
        }

        // Refresh Steam cache first
        steamService.refreshOwnedGamesCache();

        // Find all games that have a Steam App ID but haven't been synced
        List<Game> gamesToMigrate = gameRepository.findAll().stream()
                .filter(g -> g.getSteamAppId() != null && !g.getSteamAppId().isEmpty())
                .filter(g -> g.getSteamLastSynced() == null) // Only migrate unsynced games
                .toList();

        int updatedCount = 0;
        for (Game game : gamesToMigrate) {
            try {
                Optional<SteamGameInfo> steamInfo = steamService.checkOwnership(game.getSteamAppId());
                if (steamInfo.isPresent()) {
                    populateFromSteam(game, steamInfo.get());

                    // Also set platform to PC if not already set
                    if (game.getPlatform() == null) {
                        game.setPlatform(GamePlatform.PC);
                    }

                    gameRepository.save(game);
                    updatedCount++;
                    log.info("Migrated Steam data for: {} ({})", game.getTitle(), game.getSteamAppId());
                }
            } catch (Exception e) {
                log.warn("Failed to migrate Steam data for game {}: {}", game.getTitle(), e.getMessage());
            }
        }

        log.info("Steam migration completed: {} games updated", updatedCount);
        return updatedCount;
    }
}