package com.infernokun.infernoGames.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.infernokun.infernoGames.models.Game;
import com.infernokun.infernoGames.models.dto.GameRequest;
import com.infernokun.infernoGames.models.enums.GamePlatform;
import com.infernokun.infernoGames.models.enums.GameStatus;
import com.infernokun.infernoGames.services.GameService;
import com.infernokun.infernoGames.services.IGDBService.IGDBGameDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
@DisplayName("GameController Tests")
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    private ObjectMapper objectMapper;
    private Game testGame;
    private GameRequest testRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testGame = Game.builder()
                .id(1L)
                .title("Test Game")
                .description("A test game description")
                .developer("Test Developer")
                .publisher("Test Publisher")
                .releaseYear(2024)
                .genre("Action")
                .genres(List.of("Action", "Adventure"))
                .platform(GamePlatform.PC)
                .platforms(List.of(GamePlatform.PC, GamePlatform.PLAYSTATION_5))
                .status(GameStatus.NOT_STARTED)
                .rating(8)
                .playtimeHours(0.0)
                .completionPercentage(0)
                .favorite(false)
                .achievements(0)
                .totalAchievements(50)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testRequest = GameRequest.builder()
                .title("Test Game")
                .description("A test game description")
                .developer("Test Developer")
                .publisher("Test Publisher")
                .releaseYear(2024)
                .genre("Action")
                .platform(GamePlatform.PC)
                .status(GameStatus.NOT_STARTED)
                .rating(8)
                .build();
    }

    @Nested
    @DisplayName("CRUD Endpoints")
    class CrudEndpoints {

        @Test
        @DisplayName("GET /api/games should return all games")
        void getAllGames_ReturnsAllGames() throws Exception {
            Game game2 = Game.builder().id(2L).title("Another Game").build();
            when(gameService.getAllGames()).thenReturn(List.of(testGame, game2));

            mockMvc.perform(get("/api/games"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].title", is("Test Game")))
                    .andExpect(jsonPath("$.data[1].title", is("Another Game")));

            verify(gameService).getAllGames();
        }

        @Test
        @DisplayName("GET /api/games should return empty list when no games")
        void getAllGames_ReturnsEmptyList() throws Exception {
            when(gameService.getAllGames()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/games"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("GET /api/games/{id} should return game by id")
        void getGameById_ReturnsGame() throws Exception {
            when(gameService.getGameById(1L)).thenReturn(testGame);

            mockMvc.perform(get("/api/games/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id", is(1)))
                    .andExpect(jsonPath("$.data.title", is("Test Game")))
                    .andExpect(jsonPath("$.data.developer", is("Test Developer")))
                    .andExpect(jsonPath("$.data.platform", is("PC")))
                    .andExpect(jsonPath("$.data.status", is("NOT_STARTED")));
        }

        @Test
        @DisplayName("POST /api/games should create new game")
        void createGame_CreatesNewGame() throws Exception {
            when(gameService.createGame(any(GameRequest.class))).thenReturn(testGame);

            mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title", is("Test Game")))
                    .andExpect(jsonPath("$.message", is("Game created successfully")));

            verify(gameService).createGame(any(GameRequest.class));
        }

        @Test
        @DisplayName("PUT /api/games/{id} should update existing game")
        void updateGame_UpdatesGame() throws Exception {
            testGame.setTitle("Updated Title");
            when(gameService.updateGame(eq(1L), any(GameRequest.class))).thenReturn(testGame);

            GameRequest updateRequest = GameRequest.builder()
                    .title("Updated Title")
                    .build();

            mockMvc.perform(put("/api/games/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title", is("Updated Title")))
                    .andExpect(jsonPath("$.message", is("Game updated successfully")));

            verify(gameService).updateGame(eq(1L), any(GameRequest.class));
        }

        @Test
        @DisplayName("DELETE /api/games/{id} should delete game")
        void deleteGame_DeletesGame() throws Exception {
            doNothing().when(gameService).deleteGame(1L);

            mockMvc.perform(delete("/api/games/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Game deleted successfully")));

            verify(gameService).deleteGame(1L);
        }
    }

    @Nested
    @DisplayName("Status & Favorite Endpoints")
    class StatusEndpoints {

        @Test
        @DisplayName("POST /api/games/{id}/status should update game status")
        void updateGameStatus_UpdatesStatus() throws Exception {
            testGame.setStatus(GameStatus.IN_PROGRESS);
            when(gameService.updateGameStatus(1L, GameStatus.IN_PROGRESS)).thenReturn(testGame);

            mockMvc.perform(post("/api/games/1/status")
                            .param("status", "IN_PROGRESS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")))
                    .andExpect(jsonPath("$.message", is("Status updated successfully")));

            verify(gameService).updateGameStatus(1L, GameStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("POST /api/games/{id}/status with COMPLETED should set completion")
        void updateGameStatus_ToCompleted() throws Exception {
            testGame.setStatus(GameStatus.COMPLETED);
            testGame.setCompletionPercentage(100);
            when(gameService.updateGameStatus(1L, GameStatus.COMPLETED)).thenReturn(testGame);

            mockMvc.perform(post("/api/games/1/status")
                            .param("status", "COMPLETED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                    .andExpect(jsonPath("$.data.completionPercentage", is(100)));
        }

        @Test
        @DisplayName("POST /api/games/{id}/favorite should toggle favorite")
        void toggleFavorite_TogglesFavorite() throws Exception {
            testGame.setFavorite(true);
            when(gameService.toggleFavorite(1L)).thenReturn(testGame);

            mockMvc.perform(post("/api/games/1/favorite"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.favorite", is(true)));

            verify(gameService).toggleFavorite(1L);
        }
    }

    @Nested
    @DisplayName("Query Endpoints")
    class QueryEndpoints {

        @Test
        @DisplayName("GET /api/games/search should search games by query")
        void searchGames_ReturnsResults() throws Exception {
            when(gameService.searchGames("Test")).thenReturn(List.of(testGame));

            mockMvc.perform(get("/api/games/search")
                            .param("query", "Test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].title", is("Test Game")));

            verify(gameService).searchGames("Test");
        }

        @Test
        @DisplayName("GET /api/games/search/advanced should search with multiple criteria")
        void advancedSearch_UsesAllCriteria() throws Exception {
            when(gameService.advancedSearch("Test", GameStatus.IN_PROGRESS, GamePlatform.PC, "Action"))
                    .thenReturn(List.of(testGame));

            mockMvc.perform(get("/api/games/search/advanced")
                            .param("title", "Test")
                            .param("status", "IN_PROGRESS")
                            .param("platform", "PC")
                            .param("genre", "Action"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(gameService).advancedSearch("Test", GameStatus.IN_PROGRESS, GamePlatform.PC, "Action");
        }

        @Test
        @DisplayName("GET /api/games/search/advanced should work with partial criteria")
        void advancedSearch_WorksWithPartialCriteria() throws Exception {
            when(gameService.advancedSearch(null, GameStatus.COMPLETED, null, null))
                    .thenReturn(List.of(testGame));

            mockMvc.perform(get("/api/games/search/advanced")
                            .param("status", "COMPLETED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }

        @Test
        @DisplayName("GET /api/games/status/{status} should return games by status")
        void getGamesByStatus_ReturnsFilteredGames() throws Exception {
            testGame.setStatus(GameStatus.IN_PROGRESS);
            when(gameService.getGamesByStatus(GameStatus.IN_PROGRESS)).thenReturn(List.of(testGame));

            mockMvc.perform(get("/api/games/status/IN_PROGRESS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].status", is("IN_PROGRESS")));
        }

        @Test
        @DisplayName("GET /api/games/platform/{platform} should return games by platform")
        void getGamesByPlatform_ReturnsFilteredGames() throws Exception {
            when(gameService.getGamesByPlatform(GamePlatform.PC)).thenReturn(List.of(testGame));

            mockMvc.perform(get("/api/games/platform/PC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].platform", is("PC")));
        }

        @Test
        @DisplayName("GET /api/games/favorites should return favorite games")
        void getFavoriteGames_ReturnsFavorites() throws Exception {
            testGame.setFavorite(true);
            when(gameService.getFavoriteGames()).thenReturn(List.of(testGame));

            mockMvc.perform(get("/api/games/favorites"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].favorite", is(true)));
        }

        @Test
        @DisplayName("GET /api/games/recent should return recently added games")
        void getRecentlyAddedGames_ReturnsRecent() throws Exception {
            when(gameService.getRecentlyAddedGames()).thenReturn(List.of(testGame));

            mockMvc.perform(get("/api/games/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(gameService).getRecentlyAddedGames();
        }

        @Test
        @DisplayName("GET /api/games/completed should return recently completed games")
        void getRecentlyCompletedGames_ReturnsCompleted() throws Exception {
            testGame.setStatus(GameStatus.COMPLETED);
            when(gameService.getRecentlyCompletedGames()).thenReturn(List.of(testGame));

            mockMvc.perform(get("/api/games/completed"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(gameService).getRecentlyCompletedGames();
        }
    }

    @Nested
    @DisplayName("Statistics Endpoints")
    class StatisticsEndpoints {

        @Test
        @DisplayName("GET /api/games/stats should return game statistics")
        void getGameStats_ReturnsStats() throws Exception {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalGames", 10L);
            stats.put("completedGames", 5L);
            stats.put("inProgressGames", 2L);
            stats.put("notStartedGames", 2L);
            stats.put("onHoldGames", 1L);
            stats.put("droppedGames", 0L);
            stats.put("favoriteGames", 3L);
            stats.put("totalPlaytime", 150.5);
            stats.put("averageRating", 7.8);
            stats.put("completionRate", 50L);
            stats.put("platformBreakdown", Map.of("PC", 5L, "PLAYSTATION_5", 3L));
            stats.put("genreBreakdown", Map.of("Action", 4L, "RPG", 3L));

            when(gameService.getGameStats()).thenReturn(stats);

            mockMvc.perform(get("/api/games/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalGames", is(10)))
                    .andExpect(jsonPath("$.data.completedGames", is(5)))
                    .andExpect(jsonPath("$.data.totalPlaytime", is(150.5)))
                    .andExpect(jsonPath("$.data.averageRating", is(7.8)))
                    .andExpect(jsonPath("$.data.completionRate", is(50)));

            verify(gameService).getGameStats();
        }
    }

    @Nested
    @DisplayName("IGDB Integration Endpoints")
    class IgdbEndpoints {

        @Test
        @DisplayName("GET /api/games/igdb/search should search IGDB")
        void searchIGDB_ReturnsResults() throws Exception {
            IGDBGameDto igdbGame = IGDBGameDto.builder()
                    .igdbId(12345L)
                    .name("IGDB Game")
                    .summary("A great game from IGDB")
                    .build();
            when(gameService.searchIGDB("test")).thenReturn(List.of(igdbGame));

            mockMvc.perform(get("/api/games/igdb/search")
                            .param("query", "test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name", is("IGDB Game")))
                    .andExpect(jsonPath("$.data[0].igdbId", is(12345)));

            verify(gameService).searchIGDB("test");
        }

        @Test
        @DisplayName("GET /api/games/igdb/{igdbId} should return IGDB game")
        void getIGDBGameById_ReturnsGame() throws Exception {
            IGDBGameDto igdbGame = IGDBGameDto.builder()
                    .igdbId(12345L)
                    .name("IGDB Game")
                    .build();
            when(gameService.getIGDBGameById(12345L)).thenReturn(Optional.of(igdbGame));

            mockMvc.perform(get("/api/games/igdb/12345"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.igdbId", is(12345)))
                    .andExpect(jsonPath("$.data.name", is("IGDB Game")));
        }

        @Test
        @DisplayName("GET /api/games/igdb/{igdbId} should return 404 when not found")
        void getIGDBGameById_ReturnsNotFound() throws Exception {
            when(gameService.getIGDBGameById(99999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/games/igdb/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/games/igdb/popular should return popular games")
        void getPopularIGDBGames_ReturnsPopular() throws Exception {
            IGDBGameDto igdbGame = IGDBGameDto.builder()
                    .igdbId(12345L)
                    .name("Popular Game")
                    .rating(95.0)
                    .build();
            when(gameService.getPopularIGDBGames(20)).thenReturn(List.of(igdbGame));

            mockMvc.perform(get("/api/games/igdb/popular"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(gameService).getPopularIGDBGames(20);
        }

        @Test
        @DisplayName("GET /api/games/igdb/popular should respect limit parameter")
        void getPopularIGDBGames_RespectsLimit() throws Exception {
            when(gameService.getPopularIGDBGames(10)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/games/igdb/popular")
                            .param("limit", "10"))
                    .andExpect(status().isOk());

            verify(gameService).getPopularIGDBGames(10);
        }

        @Test
        @DisplayName("GET /api/games/igdb/recent should return recent releases")
        void getRecentIGDBReleases_ReturnsRecent() throws Exception {
            when(gameService.getRecentIGDBReleases(20)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/games/igdb/recent"))
                    .andExpect(status().isOk());

            verify(gameService).getRecentIGDBReleases(20);
        }

        @Test
        @DisplayName("GET /api/games/igdb/upcoming should return upcoming games")
        void getUpcomingIGDBGames_ReturnsUpcoming() throws Exception {
            when(gameService.getUpcomingIGDBGames(20)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/games/igdb/upcoming"))
                    .andExpect(status().isOk());

            verify(gameService).getUpcomingIGDBGames(20);
        }

        @Test
        @DisplayName("POST /api/games/igdb/import/{igdbId} should import game from IGDB")
        void importFromIGDB_ImportsGame() throws Exception {
            when(gameService.createGameFromIGDB(12345L)).thenReturn(testGame);

            mockMvc.perform(post("/api/games/igdb/import/12345"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title", is("Test Game")))
                    .andExpect(jsonPath("$.message", is("Game imported from IGDB successfully")));

            verify(gameService).createGameFromIGDB(12345L);
        }

        @Test
        @DisplayName("POST /api/games/{id}/igdb/refresh should refresh game from IGDB")
        void refreshFromIGDB_RefreshesGame() throws Exception {
            when(gameService.refreshFromIGDB(1L)).thenReturn(testGame);

            mockMvc.perform(post("/api/games/1/igdb/refresh"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title", is("Test Game")))
                    .andExpect(jsonPath("$.message", is("Game refreshed from IGDB successfully")));

            verify(gameService).refreshFromIGDB(1L);
        }
    }

    @Nested
    @DisplayName("Cache Management Endpoints")
    class CacheEndpoints {

        @Test
        @DisplayName("DELETE /api/games/cache should clear all caches")
        void clearCaches_ClearsAllCaches() throws Exception {
            doNothing().when(gameService).clearAllCaches();

            mockMvc.perform(delete("/api/games/cache"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Caches cleared successfully")));

            verify(gameService).clearAllCaches();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("POST /api/games with invalid rating should fail validation")
        void createGame_InvalidRating_FailsValidation() throws Exception {
            testRequest.setRating(15); // Rating should be 1-10

            mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/games with too long title should fail validation")
        void createGame_TitleTooLong_FailsValidation() throws Exception {
            testRequest.setTitle("A".repeat(256)); // Max is 255

            mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/games with invalid completion percentage should fail")
        void createGame_InvalidCompletionPercentage_FailsValidation() throws Exception {
            testRequest.setCompletionPercentage(150); // Max is 100

            mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/games with negative playtime should fail validation")
        void createGame_NegativePlaytime_FailsValidation() throws Exception {
            testRequest.setPlaytimeHours(-5.0);

            mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("GET /api/games/{id} should handle not found exception")
        void getGameById_HandlesNotFound() throws Exception {
            when(gameService.getGameById(999L))
                    .thenThrow(new IllegalArgumentException("Game not found with id: 999"));

            mockMvc.perform(get("/api/games/999"))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("POST /api/games with malformed JSON should return 400")
        void createGame_MalformedJson_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ invalid json }"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/games/{id}/status with invalid status should return 400")
        void updateGameStatus_InvalidStatus_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/games/1/status")
                            .param("status", "INVALID_STATUS"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/games/platform/{platform} with invalid platform should return 400")
        void getGamesByPlatform_InvalidPlatform_ReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/games/platform/INVALID_PLATFORM"))
                    .andExpect(status().isBadRequest());
        }
    }
}
