package com.infernokun.infernoGames.controllers;

import com.infernokun.infernoGames.models.ApiResponse;
import com.infernokun.infernoGames.models.Game;
import com.infernokun.infernoGames.models.dto.GameRequest;
import com.infernokun.infernoGames.models.enums.GamePlatform;
import com.infernokun.infernoGames.models.enums.GameStatus;
import com.infernokun.infernoGames.services.GameService;
import com.infernokun.infernoGames.services.IGDBService.IGDBGameDto;
import com.infernokun.infernoGames.services.SteamService.SteamGameInfo;
import com.infernokun.infernoGames.services.SteamService.SteamLibraryStats;
import com.infernokun.infernoGames.services.SteamService.SteamUserProfile;
import com.infernokun.infernoGames.services.SteamSyncScheduler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class GameController extends BaseController {

    private final GameService gameService;
    private final SteamSyncScheduler steamSyncScheduler;

    // ─── CRUD Endpoints ─────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<Game>>> getAllGames() {
        return createSuccessResponse(gameService.getAllGames());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Game>> getGameById(@PathVariable Long id) {
        return createSuccessResponse(gameService.getGameById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Game>> createGame(@Valid @RequestBody GameRequest request) {
        return createSuccessResponse(gameService.createGame(request), "Game created successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Game>> updateGame(
            @PathVariable Long id,
            @Valid @RequestBody GameRequest request) {
        return createSuccessResponse(gameService.updateGame(id, request), "Game updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGame(@PathVariable Long id) {
        gameService.deleteGame(id);
        return createSuccessResponse("Game deleted successfully");
    }

    // ─── Status & Favorite Endpoints ────────────────────────────────────────────

    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Game>> updateGameStatus(
            @PathVariable Long id,
            @RequestParam GameStatus status) {
        return createSuccessResponse(gameService.updateGameStatus(id, status), "Status updated successfully");
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Game>> toggleFavorite(@PathVariable Long id) {
        return createSuccessResponse(gameService.toggleFavorite(id));
    }

    // ─── Query Endpoints ────────────────────────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Game>>> searchGames(@RequestParam String query) {
        return createSuccessResponse(gameService.searchGames(query));
    }

    @GetMapping("/search/advanced")
    public ResponseEntity<ApiResponse<List<Game>>> advancedSearch(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) GameStatus status,
            @RequestParam(required = false) GamePlatform platform,
            @RequestParam(required = false) String genre) {
        return createSuccessResponse(gameService.advancedSearch(title, status, platform, genre));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<Game>>> getGamesByStatus(@PathVariable GameStatus status) {
        return createSuccessResponse(gameService.getGamesByStatus(status));
    }

    @GetMapping("/platform/{platform}")
    public ResponseEntity<ApiResponse<List<Game>>> getGamesByPlatform(@PathVariable GamePlatform platform) {
        return createSuccessResponse(gameService.getGamesByPlatform(platform));
    }

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<List<Game>>> getFavoriteGames() {
        return createSuccessResponse(gameService.getFavoriteGames());
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<Game>>> getRecentlyAddedGames() {
        return createSuccessResponse(gameService.getRecentlyAddedGames());
    }

    @GetMapping("/completed")
    public ResponseEntity<ApiResponse<List<Game>>> getRecentlyCompletedGames() {
        return createSuccessResponse(gameService.getRecentlyCompletedGames());
    }

    // ─── Statistics ─────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGameStats() {
        return createSuccessResponse(gameService.getGameStats());
    }

    // ─── IGDB Integration ───────────────────────────────────────────────────────

    @GetMapping("/igdb/search")
    public ResponseEntity<ApiResponse<List<IGDBGameDto>>> searchIGDB(@RequestParam String query) {
        return createSuccessResponse(gameService.searchIGDB(query));
    }

    @GetMapping("/igdb/{igdbId}")
    public ResponseEntity<ApiResponse<IGDBGameDto>> getIGDBGameById(@PathVariable Long igdbId) {
        return gameService.getIGDBGameById(igdbId)
                .map(this::createSuccessResponse)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/igdb/popular")
    public ResponseEntity<ApiResponse<List<IGDBGameDto>>> getPopularIGDBGames(
            @RequestParam(defaultValue = "20") int limit) {
        return createSuccessResponse(gameService.getPopularIGDBGames(limit));
    }

    @GetMapping("/igdb/recent")
    public ResponseEntity<ApiResponse<List<IGDBGameDto>>> getRecentIGDBReleases(
            @RequestParam(defaultValue = "20") int limit) {
        return createSuccessResponse(gameService.getRecentIGDBReleases(limit));
    }

    @GetMapping("/igdb/upcoming")
    public ResponseEntity<ApiResponse<List<IGDBGameDto>>> getUpcomingIGDBGames(
            @RequestParam(defaultValue = "20") int limit) {
        return createSuccessResponse(gameService.getUpcomingIGDBGames(limit));
    }

    @PostMapping("/igdb/import/{igdbId}")
    public ResponseEntity<ApiResponse<Game>> importFromIGDB(@PathVariable Long igdbId) {
        return createSuccessResponse(gameService.createGameFromIGDB(igdbId), "Game imported from IGDB successfully");
    }

    @PostMapping("/{id}/igdb/refresh")
    public ResponseEntity<ApiResponse<Game>> refreshFromIGDB(@PathVariable Long id) {
        return createSuccessResponse(gameService.refreshFromIGDB(id), "Game refreshed from IGDB successfully");
    }

    @PostMapping("/igdb/refresh-genres")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshAllGenresFromIGDB() {
        return createSuccessResponse(gameService.refreshAllGenresFromIGDB(), "Genre refresh completed");
    }

    // ─── Steam Integration ───────────────────────────────────────────────────────

    @GetMapping("/steam/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSteamStatus() {
        Map<String, Object> status = Map.of(
                "configured", gameService.isSteamConfigured(),
                "message", gameService.isSteamConfigured() ?
                        "Steam API is configured and ready" :
                        "Steam API is not configured - set STEAM_CLIENT_ID and STEAM_CLIENT_SECRET"
        );
        return createSuccessResponse(status);
    }

    @GetMapping("/steam/user")
    public ResponseEntity<ApiResponse<SteamUserProfile>> getSteamUserProfile() {
        return gameService.getSteamUserProfile()
                .map(this::createSuccessResponse)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/steam/library")
    public ResponseEntity<ApiResponse<List<SteamGameInfo>>> getSteamLibrary() {
        return createSuccessResponse(gameService.getSteamOwnedGames());
    }

    @GetMapping("/steam/library/with-genres")
    public ResponseEntity<ApiResponse<List<SteamGameInfo>>> getSteamLibraryWithGenres() {
        return createSuccessResponse(gameService.getSteamLibraryWithGenres());
    }

    @GetMapping("/steam/library/genre-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSteamGenreEnrichmentStatus() {
        Map<String, Object> status = Map.of(
                "cachedGenreCount", gameService.getCachedGenreCount(),
                "enrichmentInProgress", gameService.isEnrichmentInProgress()
        );
        return createSuccessResponse(status);
    }

    @PostMapping("/steam/library/refresh-genres")
    public ResponseEntity<ApiResponse<Void>> refreshSteamLibraryGenres() {
        gameService.clearIgdbGenreCache();
        steamSyncScheduler.triggerGenreEnrichment();
        return createSuccessResponse("Genre enrichment triggered - check status endpoint for progress");
    }

    @GetMapping("/steam/library/stats")
    public ResponseEntity<ApiResponse<SteamLibraryStats>> getSteamLibraryStats() {
        return createSuccessResponse(gameService.getSteamLibraryStats());
    }

    @GetMapping("/steam/search")
    public ResponseEntity<ApiResponse<List<SteamGameInfo>>> searchSteamLibrary(@RequestParam String query) {
        return createSuccessResponse(gameService.searchSteamGames(query));
    }

    @GetMapping("/steam/recent")
    public ResponseEntity<ApiResponse<List<SteamGameInfo>>> getRecentlyPlayedSteam(
            @RequestParam(defaultValue = "10") int count) {
        return createSuccessResponse(gameService.getRecentlyPlayedSteamGames(count));
    }

    @GetMapping("/steam/most-played")
    public ResponseEntity<ApiResponse<List<SteamGameInfo>>> getMostPlayedSteam(
            @RequestParam(defaultValue = "20") int limit) {
        return createSuccessResponse(gameService.getMostPlayedSteamGames(limit));
    }

    @GetMapping("/steam/check/{appId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSteamOwnership(@PathVariable String appId) {
        boolean owned = gameService.checkSteamOwnership(appId);
        var steamInfo = gameService.getSteamGameInfo(appId);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("appId", appId);
        result.put("owned", owned);
        result.put("gameInfo", steamInfo.orElse(null));

        return createSuccessResponse(result);
    }

    @PostMapping("/steam/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshSteamCache() {
        gameService.refreshSteamCache();
        return createSuccessResponse("Steam cache refreshed successfully");
    }

    @PostMapping("/{id}/steam/sync")
    public ResponseEntity<ApiResponse<Game>> syncGameSteamData(@PathVariable Long id) {
        return createSuccessResponse(gameService.syncGameSteamData(id), "Steam data synced successfully");
    }

    @PostMapping("/steam/sync-all")
    public ResponseEntity<ApiResponse<Void>> triggerSteamSync() {
        steamSyncScheduler.syncSteamPlaytime();
        return createSuccessResponse("Steam sync triggered successfully");
    }

    @PostMapping("/steam/validate-platforms")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateSteamPlatforms() {
        int updated = steamSyncScheduler.validateSteamPlatforms();
        return createSuccessResponse(Map.of(
                "updatedGames", updated,
                "message", updated + " games had their platform set to PC"
        ));
    }

    @PostMapping("/steam/migrate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> migrateSteamData() {
        int updated = gameService.migrateExistingSteamData();
        return createSuccessResponse(Map.of(
                "updatedGames", updated,
                "message", "Migration completed for " + updated + " games"
        ));
    }

    // ─── Cache Management ───────────────────────────────────────────────────────

    @DeleteMapping("/cache")
    public ResponseEntity<ApiResponse<Void>> clearCaches() {
        gameService.clearAllCaches();
        return createSuccessResponse("Caches cleared successfully");
    }
}