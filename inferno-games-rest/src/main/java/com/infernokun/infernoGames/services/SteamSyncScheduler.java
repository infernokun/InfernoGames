package com.infernokun.infernoGames.services;

import com.infernokun.infernoGames.models.Game;
import com.infernokun.infernoGames.models.enums.GamePlatform;
import com.infernokun.infernoGames.repositories.GameRepository;
import com.infernokun.infernoGames.services.SteamService.SteamGameInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SteamSyncScheduler {
    private final SteamService steamService;
    private final GameRepository gameRepository;
    private final GameService gameService;

    /**
     * Sync Steam playtime data every 6 hours
     * This updates playtime information for all games that have a Steam App ID
     */
    @Scheduled(fixedRateString = "PT6H", initialDelayString = "PT1M")
    @Transactional
    public void syncSteamPlaytime() {
        if (!steamService.isConfigured()) {
            log.debug("Steam sync skipped - API not configured");
            return;
        }

        log.info("Starting scheduled Steam playtime sync...");

        try {
            // Refresh the Steam cache first
            steamService.refreshOwnedGamesCache();

            // Get all games with Steam App IDs
            List<Game> steamGames = gameRepository.findAll().stream()
                    .filter(game -> game.getSteamAppId() != null && !game.getSteamAppId().isEmpty())
                    .toList();

            int updatedCount = 0;
            int errorCount = 0;

            for (Game game : steamGames) {
                try {
                    Optional<SteamGameInfo> steamInfo = steamService.checkOwnership(game.getSteamAppId());

                    if (steamInfo.isPresent()) {
                        SteamGameInfo info = steamInfo.get();
                        boolean changed = updateGameFromSteam(game, info);

                        if (changed) {
                            game.setSteamLastSynced(LocalDateTime.now());
                            gameRepository.save(game);
                            updatedCount++;
                            log.debug("Updated Steam data for: {} ({})", game.getTitle(), game.getSteamAppId());
                        }
                    }
                } catch (Exception e) {
                    errorCount++;
                    log.warn("Failed to sync Steam data for game '{}' (appId: {}): {}",
                            game.getTitle(), game.getSteamAppId(), e.getMessage());
                }
            }

            log.info("Steam sync completed: {} games updated, {} errors out of {} total Steam games",
                    updatedCount, errorCount, steamGames.size());

        } catch (Exception e) {
            log.error("Steam sync failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Update game fields from Steam data
     * Returns true if any field was changed
     */
    private boolean updateGameFromSteam(Game game, SteamGameInfo steamInfo) {
        boolean changed = false;

        // Update total playtime (convert minutes to hours)
        double newPlaytimeHours = steamInfo.getPlaytimeForever() / 60.0;
        if (game.getPlaytimeHours() == null ||
                Math.abs(game.getPlaytimeHours() - newPlaytimeHours) > 0.01) {
            game.setPlaytimeHours(newPlaytimeHours);
            changed = true;
        }

        // Update platform-specific playtimes
        if (equalsNullSafe(game.getSteamPlaytimeWindowsMinutes(), steamInfo.getPlaytimeWindowsForever())) {
            game.setSteamPlaytimeWindowsMinutes(steamInfo.getPlaytimeWindowsForever());
            changed = true;
        }

        if (equalsNullSafe(game.getSteamPlaytimeLinuxMinutes(), steamInfo.getPlaytimeLinuxForever())) {
            game.setSteamPlaytimeLinuxMinutes(steamInfo.getPlaytimeLinuxForever());
            changed = true;
        }

        if (equalsNullSafe(game.getSteamPlaytimeMacMinutes(), steamInfo.getPlaytimeMacForever())) {
            game.setSteamPlaytimeMacMinutes(steamInfo.getPlaytimeMacForever());
            changed = true;
        }

        if (equalsNullSafe(game.getSteamPlaytimeDeckMinutes(), steamInfo.getPlaytimeDeckForever())) {
            game.setSteamPlaytimeDeckMinutes(steamInfo.getPlaytimeDeckForever());
            changed = true;
        }

        // Update last played timestamp
        if (steamInfo.getRtimeLastPlayed() > 0) {
            LocalDateTime lastPlayed = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(steamInfo.getRtimeLastPlayed()),
                    ZoneId.systemDefault()
            );
            if (!lastPlayed.equals(game.getSteamLastPlayed())) {
                game.setSteamLastPlayed(lastPlayed);
                changed = true;
            }
        }

        return changed;
    }

    private boolean equalsNullSafe(Integer a, Integer b) {
        if (a == null && b == null) return false;
        if (a == null || b == null) return true;
        return !a.equals(b);
    }

    /**
     * Force a manual sync for a specific game
     */
    @Transactional
    public boolean syncSingleGame(Long gameId) {
        if (!steamService.isConfigured()) {
            log.warn("Cannot sync game - Steam API not configured");
            return false;
        }

        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) {
            log.warn("Game not found: {}", gameId);
            return false;
        }

        Game game = gameOpt.get();
        if (game.getSteamAppId() == null || game.getSteamAppId().isEmpty()) {
            log.warn("Game {} has no Steam App ID", gameId);
            return false;
        }

        Optional<SteamGameInfo> steamInfo = steamService.checkOwnership(game.getSteamAppId());
        if (steamInfo.isEmpty()) {
            log.warn("Game {} (appId: {}) not found in Steam library", gameId, game.getSteamAppId());
            return false;
        }

        boolean changed = updateGameFromSteam(game, steamInfo.get());
        game.setSteamLastSynced(LocalDateTime.now());
        gameRepository.save(game);

        log.info("Manual sync completed for game '{}': {}", game.getTitle(), changed ? "updated" : "no changes");
        return true;
    }

    /**
     * Validate and auto-set platform for games with Steam App IDs
     * This can be called to ensure all Steam-owned games have PC platform set
     */
    @Transactional
    public int validateSteamPlatforms() {
        if (!steamService.isConfigured()) {
            return 0;
        }

        List<Game> games = gameRepository.findAll().stream()
                .filter(game -> game.getSteamAppId() != null && !game.getSteamAppId().isEmpty())
                .filter(game -> game.getPlatform() == null || game.getPlatform() != GamePlatform.PC)
                .toList();

        int updatedCount = 0;
        for (Game game : games) {
            if (steamService.isGameOwned(game.getSteamAppId())) {
                game.setPlatform(GamePlatform.PC);
                gameRepository.save(game);
                updatedCount++;
                log.debug("Set platform to PC for Steam game: {}", game.getTitle());
            }
        }

        log.info("Steam platform validation completed: {} games updated", updatedCount);
        return updatedCount;
    }

    /**
     * Enrich Steam library with IGDB genres in background
     * Runs every 1 day, but only processes if not already running
     */
    @Scheduled(fixedRateString = "PT24H", initialDelayString = "PT30S")
    public void enrichSteamGenres() {
        if (!steamService.isConfigured()) {
            log.debug("Steam genre enrichment skipped - Steam API not configured");
            return;
        }

        log.debug("Starting scheduled Steam genre enrichment...");
        gameService.enrichSteamLibraryGenresInBackground();
    }

    /**
     * Manually trigger genre enrichment
     */
    public void triggerGenreEnrichment() {
        log.info("Manually triggering Steam genre enrichment");
        gameService.enrichSteamLibraryGenresInBackground();
    }
}