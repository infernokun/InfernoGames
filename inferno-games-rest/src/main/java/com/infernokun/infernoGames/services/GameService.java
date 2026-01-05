package com.infernokun.infernoGames.services;

import com.infernokun.infernoGames.models.Game;
import com.infernokun.infernoGames.models.dto.GameRequest;
import com.infernokun.infernoGames.models.enums.GamePlatform;
import com.infernokun.infernoGames.models.enums.GameStatus;
import com.infernokun.infernoGames.repositories.GameRepository;
import com.infernokun.infernoGames.services.IGDBService.IGDBGameDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final IGDBService igdbService;

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
                .platform(request.getPlatform())
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
                .build();

        log.info("Creating new game: {}", game.getTitle());
        return gameRepository.save(game);
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
        long favoriteGames = gameRepository.countByFavoriteTrue();
        Double totalPlaytime = gameRepository.getTotalPlaytime();
        Double averageRating = gameRepository.getAverageRating();

        stats.put("totalGames", totalGames);
        stats.put("completedGames", completedGames);
        stats.put("inProgressGames", inProgressGames);
        stats.put("notStartedGames", notStartedGames);
        stats.put("onHoldGames", onHoldGames);
        stats.put("droppedGames", droppedGames);
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

        Game game = Game.builder()
                .title(dto.getName())
                .description(dto.getSummary())
                .developer(dto.getDeveloper())
                .publisher(dto.getPublisher())
                .releaseYear(dto.getReleaseYear())
                .releaseDate(dto.getReleaseDate())
                .genre(dto.getGenres() != null && !dto.getGenres().isEmpty() ? dto.getGenres().get(0) : null)
                .genres(dto.getGenres() != null ? dto.getGenres() : new ArrayList<>())
                .coverImageUrl(dto.getCoverUrl())
                .screenshotUrls(dto.getScreenshotUrls() != null ? dto.getScreenshotUrls() : new ArrayList<>())
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
                .build();

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
        game.setGenre(dto.getGenres() != null && !dto.getGenres().isEmpty() ? dto.getGenres().get(0) : null);
        game.setGenres(dto.getGenres() != null ? dto.getGenres() : game.getGenres());
        game.setCoverImageUrl(dto.getCoverUrl());
        game.setScreenshotUrls(dto.getScreenshotUrls() != null ? dto.getScreenshotUrls() : game.getScreenshotUrls());
        game.setIgdbUrl(dto.getUrl());
        game.setIgdbRating(dto.getRating());
        game.setIgdbRatingCount(dto.getRatingCount());

        log.info("Refreshed game from IGDB: {} (ID: {})", game.getTitle(), gameId);
        return gameRepository.save(game);
    }

    // ─── Cache Management ───────────────────────────────────────────────────────

    @CacheEvict(value = {"games", "game", "gameStats"}, allEntries = true)
    public void clearAllCaches() {
        log.info("Cleared all game caches");
    }
}
