package com.infernokun.infernoGames.services;

import com.infernokun.infernoGames.models.Game;
import com.infernokun.infernoGames.models.dto.GameRequest;
import com.infernokun.infernoGames.models.enums.GamePlatform;
import com.infernokun.infernoGames.models.enums.GameStatus;
import com.infernokun.infernoGames.repositories.GameRepository;
import com.infernokun.infernoGames.services.IGDBService.IGDBGameDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameService Tests")
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private IGDBService igdbService;

    @InjectMocks
    private GameService gameService;

    private Game testGame;
    private GameRequest testRequest;

    @BeforeEach
    void setUp() {
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
                .genres(List.of("Action", "Adventure"))
                .platform(GamePlatform.PC)
                .platforms(List.of(GamePlatform.PC, GamePlatform.PLAYSTATION_5))
                .status(GameStatus.NOT_STARTED)
                .rating(8)
                .build();
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperations {

        @Test
        @DisplayName("getAllGames should return all games ordered by title")
        void getAllGames_ReturnsAllGames() {
            Game game2 = Game.builder().id(2L).title("Another Game").build();
            when(gameRepository.findAllByOrderByTitleAsc()).thenReturn(List.of(game2, testGame));

            List<Game> result = gameService.getAllGames();

            assertThat(result).hasSize(2);
            assertThat(result.getFirst().getTitle()).isEqualTo("Another Game");
            verify(gameRepository).findAllByOrderByTitleAsc();
        }

        @Test
        @DisplayName("getAllGames should return empty list when no games exist")
        void getAllGames_ReturnsEmptyList() {
            when(gameRepository.findAllByOrderByTitleAsc()).thenReturn(Collections.emptyList());

            List<Game> result = gameService.getAllGames();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getGameById should return game when found")
        void getGameById_ReturnsGame() {
            when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));

            Game result = gameService.getGameById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Test Game");
        }

        @Test
        @DisplayName("getGameById should throw exception when not found")
        void getGameById_ThrowsException() {
            when(gameRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gameService.getGameById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Game not found with id: 999");
        }

        @Test
        @DisplayName("createGame should create and save a new game")
        void createGame_CreatesNewGame() {
            when(gameRepository.save(any(Game.class))).thenReturn(testGame);

            Game result = gameService.createGame(testRequest);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Test Game");
            
            ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
            verify(gameRepository).save(gameCaptor.capture());
            
            Game savedGame = gameCaptor.getValue();
            assertThat(savedGame.getTitle()).isEqualTo("Test Game");
            assertThat(savedGame.getDeveloper()).isEqualTo("Test Developer");
            assertThat(savedGame.getStatus()).isEqualTo(GameStatus.NOT_STARTED);
        }

        @Test
        @DisplayName("createGame should set default values when not provided")
        void createGame_SetsDefaultValues() {
            GameRequest minimalRequest = GameRequest.builder()
                    .title("Minimal Game")
                    .build();

            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Game result = gameService.createGame(minimalRequest);

            assertThat(result.getStatus()).isEqualTo(GameStatus.NOT_STARTED);
            assertThat(result.getPlaytimeHours()).isEqualTo(0.0);
            assertThat(result.getCompletionPercentage()).isEqualTo(0);
            assertThat(result.getFavorite()).isFalse();
            assertThat(result.getAchievements()).isEqualTo(0);
            assertThat(result.getTotalAchievements()).isEqualTo(0);
            assertThat(result.getGenres()).isEmpty();
            assertThat(result.getPlatforms()).isEmpty();
            assertThat(result.getScreenshotUrls()).isEmpty();
        }

        @Test
        @DisplayName("updateGame should update existing game fields")
        void updateGame_UpdatesFields() {
            when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

            GameRequest updateRequest = GameRequest.builder()
                    .title("Updated Title")
                    .rating(9)
                    .playtimeHours(25.5)
                    .build();

            Game result = gameService.updateGame(1L, updateRequest);

            assertThat(result.getTitle()).isEqualTo("Updated Title");
            assertThat(result.getRating()).isEqualTo(9);
            assertThat(result.getPlaytimeHours()).isEqualTo(25.5);
            // Original values should be preserved
            assertThat(result.getDeveloper()).isEqualTo("Test Developer");
        }

        @Test
        @DisplayName("updateGame should update status with timestamp")
        void updateGame_UpdatesStatusWithTimestamp() {
            testGame.setStatus(GameStatus.NOT_STARTED);
            testGame.setStartedAt(null);
            
            when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

            GameRequest updateRequest = GameRequest.builder()
                    .status(GameStatus.IN_PROGRESS)
                    .build();

            Game result = gameService.updateGame(1L, updateRequest);

            assertThat(result.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(result.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("deleteGame should delete existing game")
        void deleteGame_DeletesGame() {
            when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
            doNothing().when(gameRepository).delete(any(Game.class));

            gameService.deleteGame(1L);

            verify(gameRepository).delete(testGame);
        }

        @Test
        @DisplayName("deleteGame should throw exception when game not found")
        void deleteGame_ThrowsExceptionWhenNotFound() {
            when(gameRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gameService.deleteGame(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Game not found with id: 999");
            
            verify(gameRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Status Operations")
    class StatusOperations {

        @Test
        @DisplayName("updateGameStatus should update to IN_PROGRESS and set startedAt")
        void updateGameStatus_ToInProgress() {
            testGame.setStartedAt(null);
            when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Game result = gameService.updateGameStatus(1L, GameStatus.IN_PROGRESS);

            assertThat(result.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(result.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("updateGameStatus should update to COMPLETED and set completedAt")
        void updateGameStatus_ToCompleted() {
            testGame.setCompletedAt(null);
            when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Game result = gameService.updateGameStatus(1L, GameStatus.COMPLETED);

            assertThat(result.getStatus()).isEqualTo(GameStatus.COMPLETED);
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(result.getCompletionPercentage()).isEqualTo(100);
        }

        @Test
        @DisplayName("updateGameStatus should not override existing startedAt")
        void updateGameStatus_PreservesExistingStartedAt() {
            LocalDateTime originalStartedAt = LocalDateTime.of(2024, 1, 1, 10, 0);
            testGame.setStartedAt(originalStartedAt);
            when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Game result = gameService.updateGameStatus(1L, GameStatus.IN_PROGRESS);

            assertThat(result.getStartedAt()).isEqualTo(originalStartedAt);
        }

        @Test
        @DisplayName("toggleFavorite should toggle favorite status")
        void toggleFavorite_TogglesFavorite() {
            testGame.setFavorite(false);
            when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Game result = gameService.toggleFavorite(1L);
            assertThat(result.getFavorite()).isTrue();

            // Toggle again
            result = gameService.toggleFavorite(1L);
            assertThat(result.getFavorite()).isFalse();
        }
    }

    @Nested
    @DisplayName("Query Operations")
    class QueryOperations {

        @Test
        @DisplayName("searchGames should find games by title")
        void searchGames_FindsByTitle() {
            when(gameRepository.findByTitleContainingIgnoreCase("Test"))
                    .thenReturn(List.of(testGame));

            List<Game> result = gameService.searchGames("Test");

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getTitle()).isEqualTo("Test Game");
        }

        @Test
        @DisplayName("getGamesByStatus should return games with specific status")
        void getGamesByStatus_ReturnsFilteredGames() {
            when(gameRepository.findByStatus(GameStatus.IN_PROGRESS))
                    .thenReturn(List.of(testGame));

            List<Game> result = gameService.getGamesByStatus(GameStatus.IN_PROGRESS);

            assertThat(result).hasSize(1);
            verify(gameRepository).findByStatus(GameStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("getGamesByPlatform should return games for specific platform")
        void getGamesByPlatform_ReturnsFilteredGames() {
            when(gameRepository.findByPlatform(GamePlatform.PC))
                    .thenReturn(List.of(testGame));

            List<Game> result = gameService.getGamesByPlatform(GamePlatform.PC);

            assertThat(result).hasSize(1);
            verify(gameRepository).findByPlatform(GamePlatform.PC);
        }

        @Test
        @DisplayName("getFavoriteGames should return only favorite games")
        void getFavoriteGames_ReturnsFavorites() {
            testGame.setFavorite(true);
            when(gameRepository.findByFavoriteTrue()).thenReturn(List.of(testGame));

            List<Game> result = gameService.getFavoriteGames();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getFavorite()).isTrue();
        }

        @Test
        @DisplayName("advancedSearch should use all criteria")
        void advancedSearch_UsesAllCriteria() {
            when(gameRepository.searchGames("Test", GameStatus.IN_PROGRESS, GamePlatform.PC, "Action"))
                    .thenReturn(List.of(testGame));

            List<Game> result = gameService.advancedSearch("Test", GameStatus.IN_PROGRESS, GamePlatform.PC, "Action");

            assertThat(result).hasSize(1);
            verify(gameRepository).searchGames("Test", GameStatus.IN_PROGRESS, GamePlatform.PC, "Action");
        }

        @Test
        @DisplayName("getRecentlyAddedGames should return most recently added")
        void getRecentlyAddedGames_ReturnsRecent() {
            when(gameRepository.findTop10ByOrderByCreatedAtDesc())
                    .thenReturn(List.of(testGame));

            List<Game> result = gameService.getRecentlyAddedGames();

            assertThat(result).hasSize(1);
            verify(gameRepository).findTop10ByOrderByCreatedAtDesc();
        }

        @Test
        @DisplayName("getRecentlyCompletedGames should return completed games")
        void getRecentlyCompletedGames_ReturnsCompleted() {
            testGame.setStatus(GameStatus.COMPLETED);
            when(gameRepository.findByStatusOrderByCompletedAtDesc(GameStatus.COMPLETED))
                    .thenReturn(List.of(testGame));

            List<Game> result = gameService.getRecentlyCompletedGames();

            assertThat(result).hasSize(1);
            verify(gameRepository).findByStatusOrderByCompletedAtDesc(GameStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Statistics")
    class Statistics {

        @Test
        @DisplayName("getGameStats should calculate all statistics")
        void getGameStats_CalculatesStats() {
            when(gameRepository.count()).thenReturn(10L);
            when(gameRepository.countByStatus(GameStatus.COMPLETED)).thenReturn(5L);
            when(gameRepository.countByStatus(GameStatus.IN_PROGRESS)).thenReturn(2L);
            when(gameRepository.countByStatus(GameStatus.NOT_STARTED)).thenReturn(2L);
            when(gameRepository.countByStatus(GameStatus.ON_HOLD)).thenReturn(1L);
            when(gameRepository.countByStatus(GameStatus.DROPPED)).thenReturn(0L);
            when(gameRepository.countByFavoriteTrue()).thenReturn(3L);
            when(gameRepository.getTotalPlaytime()).thenReturn(150.5);
            when(gameRepository.getAverageRating()).thenReturn(7.8);
            when(gameRepository.findByPlatform(any())).thenReturn(Collections.emptyList());
            when(gameRepository.findAllByOrderByTitleAsc()).thenReturn(List.of(testGame));

            Map<String, Object> stats = gameService.getGameStats();

            assertThat(stats.get("totalGames")).isEqualTo(10L);
            assertThat(stats.get("completedGames")).isEqualTo(5L);
            assertThat(stats.get("inProgressGames")).isEqualTo(2L);
            assertThat(stats.get("notStartedGames")).isEqualTo(2L);
            assertThat(stats.get("onHoldGames")).isEqualTo(1L);
            assertThat(stats.get("droppedGames")).isEqualTo(0L);
            assertThat(stats.get("favoriteGames")).isEqualTo(3L);
            assertThat(stats.get("totalPlaytime")).isEqualTo(150.5);
            assertThat(stats.get("averageRating")).isEqualTo(7.8);
            assertThat(stats.get("completionRate")).isEqualTo(50L);
        }

        @Test
        @DisplayName("getGameStats should handle null values gracefully")
        void getGameStats_HandlesNullValues() {
            when(gameRepository.count()).thenReturn(0L);
            when(gameRepository.countByStatus(any())).thenReturn(0L);
            when(gameRepository.countByFavoriteTrue()).thenReturn(0L);
            when(gameRepository.getTotalPlaytime()).thenReturn(null);
            when(gameRepository.getAverageRating()).thenReturn(null);
            when(gameRepository.findByPlatform(any())).thenReturn(Collections.emptyList());
            when(gameRepository.findAllByOrderByTitleAsc()).thenReturn(Collections.emptyList());

            Map<String, Object> stats = gameService.getGameStats();

            assertThat(stats.get("totalPlaytime")).isEqualTo(0.0);
            assertThat(stats.get("averageRating")).isEqualTo(0.0);
            assertThat(stats.get("completionRate")).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("IGDB Integration")
    class IgdbIntegration {

        @Test
        @DisplayName("searchIGDB should delegate to IGDBService")
        void searchIGDB_DelegatesToService() {
            IGDBGameDto igdbGame = IGDBGameDto.builder()
                    .igdbId(12345L)
                    .name("IGDB Game")
                    .build();
            when(igdbService.searchGames("test")).thenReturn(List.of(igdbGame));

            List<IGDBGameDto> result = gameService.searchIGDB("test");

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getName()).isEqualTo("IGDB Game");
            verify(igdbService).searchGames("test");
        }

        @Test
        @DisplayName("getIGDBGameById should return game from IGDB")
        void getIGDBGameById_ReturnsGame() {
            IGDBGameDto igdbGame = IGDBGameDto.builder()
                    .igdbId(12345L)
                    .name("IGDB Game")
                    .build();
            when(igdbService.getGameById(12345L)).thenReturn(Optional.of(igdbGame));

            Optional<IGDBGameDto> result = gameService.getIGDBGameById(12345L);

            assertThat(result).isPresent();
            assertThat(result.get().getIgdbId()).isEqualTo(12345L);
        }

        @Test
        @DisplayName("createGameFromIGDB should return existing game if already imported")
        void createGameFromIGDB_ReturnsExistingGame() {
            testGame.setIgdbId(12345L);
            when(gameRepository.findByIgdbId(12345L)).thenReturn(Optional.of(testGame));

            Game result = gameService.createGameFromIGDB(12345L);

            assertThat(result).isEqualTo(testGame);
            verify(igdbService, never()).getGameById(any());
            verify(gameRepository, never()).save(any());
        }

        @Test
        @DisplayName("createGameFromIGDB should create new game from IGDB data")
        void createGameFromIGDB_CreatesNewGame() {
            IGDBGameDto igdbGame = IGDBGameDto.builder()
                    .igdbId(12345L)
                    .name("IGDB Game")
                    .summary("A great game")
                    .developer("IGDB Developer")
                    .publisher("IGDB Publisher")
                    .releaseYear(2024)
                    .genres(List.of("Action", "RPG"))
                    .coverUrl("https://example.com/cover.jpg")
                    .screenshotUrls(List.of("https://example.com/screenshot.jpg"))
                    .url("https://igdb.com/games/12345")
                    .rating(85.5)
                    .ratingCount(1000)
                    .build();

            when(gameRepository.findByIgdbId(12345L)).thenReturn(Optional.empty());
            when(igdbService.getGameById(12345L)).thenReturn(Optional.of(igdbGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
                Game game = invocation.getArgument(0);
                game.setId(1L);
                return game;
            });

            Game result = gameService.createGameFromIGDB(12345L);

            assertThat(result.getTitle()).isEqualTo("IGDB Game");
            assertThat(result.getDescription()).isEqualTo("A great game");
            assertThat(result.getDeveloper()).isEqualTo("IGDB Developer");
            assertThat(result.getIgdbId()).isEqualTo(12345L);
            assertThat(result.getStatus()).isEqualTo(GameStatus.NOT_STARTED);
            verify(gameRepository).save(any(Game.class));
        }

        @Test
        @DisplayName("createGameFromIGDB should throw exception when game not found in IGDB")
        void createGameFromIGDB_ThrowsExceptionWhenNotFound() {
            when(gameRepository.findByIgdbId(99999L)).thenReturn(Optional.empty());
            when(igdbService.getGameById(99999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gameService.createGameFromIGDB(99999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Game not found in IGDB with id: 99999");
        }

        @Test
        @DisplayName("refreshFromIGDB should update game with IGDB data")
        void refreshFromIGDB_UpdatesGame() {
            testGame.setIgdbId(12345L);
            IGDBGameDto igdbGame = IGDBGameDto.builder()
                    .igdbId(12345L)
                    .name("Updated IGDB Game")
                    .summary("Updated summary")
                    .developer("Updated Developer")
                    .releaseYear(2025)
                    .genres(List.of("Strategy"))
                    .build();

            when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
            when(igdbService.getGameById(12345L)).thenReturn(Optional.of(igdbGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Game result = gameService.refreshFromIGDB(1L);

            assertThat(result.getDescription()).isEqualTo("Updated summary");
            assertThat(result.getDeveloper()).isEqualTo("Updated Developer");
            assertThat(result.getReleaseYear()).isEqualTo(2025);
        }

        @Test
        @DisplayName("refreshFromIGDB should throw exception when game has no IGDB ID")
        void refreshFromIGDB_ThrowsExceptionWhenNoIgdbId() {
            testGame.setIgdbId(null);
            when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));

            assertThatThrownBy(() -> gameService.refreshFromIGDB(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Game has no IGDB ID associated");
        }

        @Test
        @DisplayName("getPopularIGDBGames should delegate to IGDBService")
        void getPopularIGDBGames_DelegatesToService() {
            when(igdbService.getPopularGames(20)).thenReturn(Collections.emptyList());

            gameService.getPopularIGDBGames(20);

            verify(igdbService).getPopularGames(20);
        }

        @Test
        @DisplayName("getRecentIGDBReleases should delegate to IGDBService")
        void getRecentIGDBReleases_DelegatesToService() {
            when(igdbService.getRecentReleases(20)).thenReturn(Collections.emptyList());

            gameService.getRecentIGDBReleases(20);

            verify(igdbService).getRecentReleases(20);
        }

        @Test
        @DisplayName("getUpcomingIGDBGames should delegate to IGDBService")
        void getUpcomingIGDBGames_DelegatesToService() {
            when(igdbService.getUpcomingGames(20)).thenReturn(Collections.emptyList());

            gameService.getUpcomingIGDBGames(20);

            verify(igdbService).getUpcomingGames(20);
        }
    }

    @Nested
    @DisplayName("Cache Management")
    class CacheManagement {

        @Test
        @DisplayName("clearAllCaches should execute without error")
        void clearAllCaches_ExecutesWithoutError() {
            assertThatNoException().isThrownBy(() -> gameService.clearAllCaches());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("getGameByIgdbId should return empty when not found")
        void getGameByIgdbId_ReturnsEmptyWhenNotFound() {
            when(gameRepository.findByIgdbId(99999L)).thenReturn(Optional.empty());

            Optional<Game> result = gameService.getGameByIgdbId(99999L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("createGame should handle null lists in request")
        void createGame_HandlesNullLists() {
            GameRequest requestWithNullLists = GameRequest.builder()
                    .title("Game with nulls")
                    .genres(null)
                    .platforms(null)
                    .screenshotUrls(null)
                    .build();

            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Game result = gameService.createGame(requestWithNullLists);

            assertThat(result.getGenres()).isEmpty();
            assertThat(result.getPlatforms()).isEmpty();
            assertThat(result.getScreenshotUrls()).isEmpty();
        }

        @Test
        @DisplayName("updateGame should only update provided fields")
        void updateGame_OnlyUpdatesProvidedFields() {
            when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Only update title, leave everything else null
            GameRequest partialUpdate = GameRequest.builder()
                    .title("Only Title Changed")
                    .build();

            Game result = gameService.updateGame(1L, partialUpdate);

            assertThat(result.getTitle()).isEqualTo("Only Title Changed");
            assertThat(result.getDeveloper()).isEqualTo("Test Developer"); // unchanged
            assertThat(result.getPublisher()).isEqualTo("Test Publisher"); // unchanged
            assertThat(result.getRating()).isEqualTo(8); // unchanged
        }
    }
}
