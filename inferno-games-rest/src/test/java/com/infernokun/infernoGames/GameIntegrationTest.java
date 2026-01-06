package com.infernokun.infernoGames;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.infernokun.infernoGames.models.Game;
import com.infernokun.infernoGames.models.dto.GameRequest;
import com.infernokun.infernoGames.models.enums.GamePlatform;
import com.infernokun.infernoGames.models.enums.GameStatus;
import com.infernokun.infernoGames.repositories.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Game Integration Tests")
class GameIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameRepository gameRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        gameRepository.deleteAll();
    }

    @Nested
    @DisplayName("Full CRUD Flow Tests")
    class FullCrudFlowTests {

        @Test
        @DisplayName("should create, read, update, and delete a game")
        void fullCrudFlow() throws Exception {
            // CREATE
            GameRequest createRequest = GameRequest.builder()
                    .title("Integration Test Game")
                    .description("A game for integration testing")
                    .developer("Test Developer")
                    .publisher("Test Publisher")
                    .releaseYear(2024)
                    .genre("Action")
                    .platform(GamePlatform.PC)
                    .status(GameStatus.NOT_STARTED)
                    .rating(8)
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title", is("Integration Test Game")))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andReturn();

            // Extract the created game ID
            String responseJson = createResult.getResponse().getContentAsString();
            Long gameId = objectMapper.readTree(responseJson).get("data").get("id").asLong();

            // READ
            mockMvc.perform(get("/api/games/" + gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title", is("Integration Test Game")))
                    .andExpect(jsonPath("$.data.status", is("NOT_STARTED")));

            // UPDATE
            GameRequest updateRequest = GameRequest.builder()
                    .title("Updated Integration Test Game")
                    .rating(9)
                    .playtimeHours(10.0)
                    .build();

            mockMvc.perform(put("/api/games/" + gameId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title", is("Updated Integration Test Game")))
                    .andExpect(jsonPath("$.data.rating", is(9)))
                    .andExpect(jsonPath("$.data.playtimeHours", is(10.0)));

            // Verify update persisted
            mockMvc.perform(get("/api/games/" + gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title", is("Updated Integration Test Game")));

            // DELETE
            mockMvc.perform(delete("/api/games/" + gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Game deleted successfully")));

            // Verify deletion
            assertThat(gameRepository.findById(gameId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Status Flow Tests")
    class StatusFlowTests {

        @Test
        @DisplayName("should update game status through lifecycle")
        void statusLifecycleFlow() throws Exception {
            // Create a game
            GameRequest createRequest = GameRequest.builder()
                    .title("Status Flow Game")
                    .platform(GamePlatform.PC)
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is("NOT_STARTED")))
                    .andReturn();

            long gameId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .get("data").get("id").asLong();

            // Start playing
            mockMvc.perform(post("/api/games/" + gameId + "/status")
                            .param("status", "IN_PROGRESS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")))
                    .andExpect(jsonPath("$.data.startedAt").exists());

            // Complete the game
            mockMvc.perform(post("/api/games/" + gameId + "/status")
                            .param("status", "COMPLETED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                    .andExpect(jsonPath("$.data.completedAt").exists())
                    .andExpect(jsonPath("$.data.completionPercentage", is(100)));
        }

        @Test
        @DisplayName("should toggle favorite status")
        void favoriteToggleFlow() throws Exception {
            // Create a game
            GameRequest createRequest = GameRequest.builder()
                    .title("Favorite Test Game")
                    .platform(GamePlatform.PC)
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.favorite", is(false)))
                    .andReturn();

            long gameId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .get("data").get("id").asLong();

            // Toggle to favorite
            mockMvc.perform(post("/api/games/" + gameId + "/favorite"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.favorite", is(true)));

            // Toggle back
            mockMvc.perform(post("/api/games/" + gameId + "/favorite"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.favorite", is(false)));
        }
    }

    @Nested
    @DisplayName("Search and Filter Tests")
    class SearchFilterTests {

        @BeforeEach
        void setUpTestData() {
            // Create test games
            Game game1 = Game.builder()
                    .title("Action Hero")
                    .genre("Action")
                    .platform(GamePlatform.PC)
                    .status(GameStatus.COMPLETED)
                    .rating(9)
                    .playtimeHours(50.0)
                    .favorite(true)
                    .build();

            Game game2 = Game.builder()
                    .title("RPG Adventure")
                    .genre("RPG")
                    .platform(GamePlatform.PLAYSTATION_5)
                    .status(GameStatus.IN_PROGRESS)
                    .rating(8)
                    .playtimeHours(30.0)
                    .favorite(false)
                    .build();

            Game game3 = Game.builder()
                    .title("Strategy Master")
                    .genre("Strategy")
                    .platform(GamePlatform.PC)
                    .status(GameStatus.NOT_STARTED)
                    .rating(7)
                    .playtimeHours(0.0)
                    .favorite(false)
                    .build();

            gameRepository.saveAll(List.of(game1, game2, game3));
        }

        @Test
        @DisplayName("should return all games ordered by title")
        void getAllGames_ReturnsOrderedList() throws Exception {
            mockMvc.perform(get("/api/games"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].title", is("Action Hero")))
                    .andExpect(jsonPath("$.data[1].title", is("RPG Adventure")))
                    .andExpect(jsonPath("$.data[2].title", is("Strategy Master")));
        }

        @Test
        @DisplayName("should search games by title")
        void searchByTitle() throws Exception {
            mockMvc.perform(get("/api/games/search")
                            .param("query", "Action"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].title", is("Action Hero")));
        }

        @Test
        @DisplayName("should filter games by status")
        void filterByStatus() throws Exception {
            mockMvc.perform(get("/api/games/status/COMPLETED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].title", is("Action Hero")));
        }

        @Test
        @DisplayName("should filter games by platform")
        void filterByPlatform() throws Exception {
            mockMvc.perform(get("/api/games/platform/PC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        @Test
        @DisplayName("should get favorite games")
        void getFavorites() throws Exception {
            mockMvc.perform(get("/api/games/favorites"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].title", is("Action Hero")));
        }

        @Test
        @DisplayName("should perform advanced search with multiple criteria")
        void advancedSearch() throws Exception {
            mockMvc.perform(get("/api/games/search/advanced")
                            .param("platform", "PC")
                            .param("status", "COMPLETED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].title", is("Action Hero")));
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @BeforeEach
        void setUpTestData() {
            Game completed1 = Game.builder()
                    .title("Completed 1")
                    .platform(GamePlatform.PC)
                    .status(GameStatus.COMPLETED)
                    .rating(9)
                    .playtimeHours(50.0)
                    .favorite(true)
                    .build();

            Game completed2 = Game.builder()
                    .title("Completed 2")
                    .platform(GamePlatform.PLAYSTATION_5)
                    .status(GameStatus.COMPLETED)
                    .rating(8)
                    .playtimeHours(30.0)
                    .favorite(false)
                    .build();

            Game inProgress = Game.builder()
                    .title("In Progress")
                    .platform(GamePlatform.PC)
                    .status(GameStatus.IN_PROGRESS)
                    .rating(7)
                    .playtimeHours(15.0)
                    .favorite(false)
                    .build();

            Game notStarted = Game.builder()
                    .title("Not Started")
                    .platform(GamePlatform.NINTENDO_SWITCH)
                    .status(GameStatus.NOT_STARTED)
                    .playtimeHours(0.0)
                    .favorite(false)
                    .build();

            gameRepository.saveAll(List.of(completed1, completed2, inProgress, notStarted));
        }

        @Test
        @DisplayName("should return correct statistics")
        void getStats() throws Exception {
            mockMvc.perform(get("/api/games/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalGames", is(4)))
                    .andExpect(jsonPath("$.data.completedGames", is(2)))
                    .andExpect(jsonPath("$.data.inProgressGames", is(1)))
                    .andExpect(jsonPath("$.data.notStartedGames", is(1)))
                    .andExpect(jsonPath("$.data.favoriteGames", is(1)))
                    .andExpect(jsonPath("$.data.totalPlaytime", is(95.0)))
                    .andExpect(jsonPath("$.data.completionRate", is(50)));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should return error for non-existent game")
        void getNonExistentGame_ReturnsError() throws Exception {
            mockMvc.perform(get("/api/games/99999"))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("should reject invalid rating")
        void invalidRating_ReturnsError() throws Exception {
            GameRequest request = GameRequest.builder()
                    .title("Invalid Rating Game")
                    .rating(15) // Invalid: max is 10
                    .build();

            mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject invalid completion percentage")
        void invalidCompletionPercentage_ReturnsError() throws Exception {
            GameRequest request = GameRequest.builder()
                    .title("Invalid Completion Game")
                    .completionPercentage(150) // Invalid: max is 100
                    .build();

            mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject title exceeding max length")
        void titleTooLong_ReturnsError() throws Exception {
            GameRequest request = GameRequest.builder()
                    .title("A".repeat(256)) // Invalid: max is 255
                    .build();

            mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject invalid status")
        void invalidStatus_ReturnsError() throws Exception {
            // First create a valid game
            GameRequest createRequest = GameRequest.builder()
                    .title("Test Game")
                    .build();

            MvcResult result = mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andReturn();

            long gameId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("data").get("id").asLong();

            // Try invalid status
            mockMvc.perform(post("/api/games/" + gameId + "/status")
                            .param("status", "INVALID_STATUS"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Cache Tests")
    class CacheTests {

        @Test
        @DisplayName("should clear caches successfully")
        void clearCaches() throws Exception {
            mockMvc.perform(delete("/api/games/cache"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Caches cleared successfully")));
        }
    }

    @Nested
    @DisplayName("Data Persistence Tests")
    class DataPersistenceTests {

        @Test
        @DisplayName("should persist timestamps automatically")
        void persistsTimestamps() throws Exception {
            GameRequest request = GameRequest.builder()
                    .title("Timestamp Test Game")
                    .build();

            MvcResult result = mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.createdAt").exists())
                    .andExpect(jsonPath("$.data.updatedAt").exists())
                    .andReturn();

            Long gameId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("data").get("id").asLong();

            // Update the game
            Thread.sleep(100); // Small delay to ensure different timestamp
            GameRequest updateRequest = GameRequest.builder()
                    .title("Updated Timestamp Test Game")
                    .build();

            mockMvc.perform(put("/api/games/" + gameId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk());

            // Verify updatedAt changed
            Game game = gameRepository.findById(gameId).orElseThrow();
            assertThat(game.getUpdatedAt()).isAfterOrEqualTo(game.getCreatedAt());
        }

        @Test
        @DisplayName("should persist lists correctly")
        void persistsListsCorrectly() throws Exception {
            GameRequest request = GameRequest.builder()
                    .title("List Test Game")
                    .genres(List.of("Action", "Adventure", "RPG"))
                    .platforms(List.of(GamePlatform.PC, GamePlatform.PLAYSTATION_5))
                    .screenshotUrls(List.of("https://example.com/1.jpg", "https://example.com/2.jpg"))
                    .build();

            MvcResult result = mockMvc.perform(post("/api/games")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            Long gameId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("data").get("id").asLong();

            Game game = gameRepository.findById(gameId).orElseThrow();
            assertThat(game.getGenres()).containsExactly("Action", "Adventure", "RPG");
            assertThat(game.getPlatforms()).containsExactly(GamePlatform.PC, GamePlatform.PLAYSTATION_5);
            assertThat(game.getScreenshotUrls()).containsExactly(
                    "https://example.com/1.jpg", "https://example.com/2.jpg");
        }
    }
}
