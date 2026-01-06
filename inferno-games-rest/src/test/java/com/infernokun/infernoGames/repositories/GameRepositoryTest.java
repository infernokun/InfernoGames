package com.infernokun.infernoGames.repositories;

import com.infernokun.infernoGames.models.Game;
import com.infernokun.infernoGames.models.enums.GamePlatform;
import com.infernokun.infernoGames.models.enums.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("GameRepository Tests")
class GameRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GameRepository gameRepository;

    private Game game1;
    private Game game2;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        gameRepository.deleteAll();
        entityManager.flush();

        // Create test games
        game1 = Game.builder()
                .title("Alpha Game")
                .description("First test game")
                .developer("Developer A")
                .publisher("Publisher A")
                .releaseYear(2024)
                .genre("Action")
                .platform(GamePlatform.PC)
                .status(GameStatus.COMPLETED)
                .rating(9)
                .playtimeHours(50.0)
                .completionPercentage(100)
                .favorite(true)
                .achievements(50)
                .totalAchievements(50)
                .igdbId(1001L)
                .completedAt(LocalDateTime.now().minusDays(5))
                .build();

        game2 = Game.builder()
                .title("Beta Game")
                .description("Second test game")
                .developer("Developer B")
                .publisher("Publisher B")
                .releaseYear(2023)
                .genre("RPG")
                .platform(GamePlatform.PLAYSTATION_5)
                .status(GameStatus.IN_PROGRESS)
                .rating(8)
                .playtimeHours(30.0)
                .completionPercentage(60)
                .favorite(false)
                .achievements(20)
                .totalAchievements(40)
                .igdbId(1002L)
                .startedAt(LocalDateTime.now().minusDays(10))
                .build();

        // No rating yet
        Game game3 = Game.builder()
                .title("Gamma Test")
                .description("Third test game")
                .developer("Developer A")
                .publisher("Publisher C")
                .releaseYear(2024)
                .genre("Action")
                .platform(GamePlatform.PC)
                .status(GameStatus.NOT_STARTED)
                .rating(null) // No rating yet
                .playtimeHours(0.0)
                .completionPercentage(0)
                .favorite(false)
                .achievements(0)
                .totalAchievements(25)
                .igdbId(1003L)
                .build();

        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.persist(game3);
        entityManager.flush();
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudOperations {

        @Test
        @DisplayName("save should persist new game")
        void save_PersistsNewGame() {
            Game newGame = Game.builder()
                    .title("New Game")
                    .platform(GamePlatform.NINTENDO_SWITCH)
                    .status(GameStatus.NOT_STARTED)
                    .build();

            Game saved = gameRepository.save(newGame);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getTitle()).isEqualTo("New Game");
        }

        @Test
        @DisplayName("findById should return game when exists")
        void findById_ReturnsGameWhenExists() {
            Optional<Game> found = gameRepository.findById(game1.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Alpha Game");
        }

        @Test
        @DisplayName("findById should return empty when not exists")
        void findById_ReturnsEmptyWhenNotExists() {
            Optional<Game> found = gameRepository.findById(99999L);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("delete should remove game")
        void delete_RemovesGame() {
            Long gameId = game1.getId();
            
            gameRepository.delete(game1);
            entityManager.flush();

            Optional<Game> found = gameRepository.findById(gameId);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("count should return correct count")
        void count_ReturnsCorrectCount() {
            long count = gameRepository.count();

            assertThat(count).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("IGDB Operations")
    class IgdbOperations {

        @Test
        @DisplayName("findByIgdbId should return game when exists")
        void findByIgdbId_ReturnsGameWhenExists() {
            Optional<Game> found = gameRepository.findByIgdbId(1001L);

            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Alpha Game");
        }

        @Test
        @DisplayName("findByIgdbId should return empty when not exists")
        void findByIgdbId_ReturnsEmptyWhenNotExists() {
            Optional<Game> found = gameRepository.findByIgdbId(99999L);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("existsByIgdbId should return true when exists")
        void existsByIgdbId_ReturnsTrueWhenExists() {
            boolean exists = gameRepository.existsByIgdbId(1001L);

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("existsByIgdbId should return false when not exists")
        void existsByIgdbId_ReturnsFalseWhenNotExists() {
            boolean exists = gameRepository.existsByIgdbId(99999L);

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Search Operations")
    class SearchOperations {

        @Test
        @DisplayName("findByTitleContainingIgnoreCase should find games by title")
        void findByTitleContainingIgnoreCase_FindsGames() {
            List<Game> found = gameRepository.findByTitleContainingIgnoreCase("game");

            assertThat(found).hasSize(2);
            assertThat(found).extracting(Game::getTitle)
                    .containsExactlyInAnyOrder("Alpha Game", "Beta Game");
        }

        @Test
        @DisplayName("findByTitleContainingIgnoreCase should be case insensitive")
        void findByTitleContainingIgnoreCase_IsCaseInsensitive() {
            List<Game> found = gameRepository.findByTitleContainingIgnoreCase("ALPHA");

            assertThat(found).hasSize(1);
            assertThat(found.getFirst().getTitle()).isEqualTo("Alpha Game");
        }

        @Test
        @DisplayName("findByTitleContainingIgnoreCase should return empty for no matches")
        void findByTitleContainingIgnoreCase_ReturnsEmptyForNoMatches() {
            List<Game> found = gameRepository.findByTitleContainingIgnoreCase("xyz");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("searchGames should filter by all criteria")
        void searchGames_FiltersByAllCriteria() {
            List<Game> found = gameRepository.searchGames(
                    "Alpha", GameStatus.COMPLETED, GamePlatform.PC, "Action");

            assertThat(found).hasSize(1);
            assertThat(found.getFirst().getTitle()).isEqualTo("Alpha Game");
        }

        @Test
        @DisplayName("searchGames should handle null title")
        void searchGames_HandlesNullTitle() {
            List<Game> found = gameRepository.searchGames(
                    null, GameStatus.COMPLETED, null, null);

            assertThat(found).hasSize(1);
            assertThat(found.getFirst().getTitle()).isEqualTo("Alpha Game");
        }

        @Test
        @DisplayName("searchGames should handle null status")
        void searchGames_HandlesNullStatus() {
            List<Game> found = gameRepository.searchGames(
                    "Game", null, null, null);

            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("searchGames should handle all null parameters")
        void searchGames_HandlesAllNullParameters() {
            List<Game> found = gameRepository.searchGames(null, null, null, null);

            assertThat(found).hasSize(3);
        }

        @Test
        @DisplayName("findByGenreContainingIgnoreCase should find games by genre")
        void findByGenreContainingIgnoreCase_FindsGames() {
            List<Game> found = gameRepository.findByGenreContainingIgnoreCase("action");

            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("findByDeveloperContainingIgnoreCase should find games by developer")
        void findByDeveloperContainingIgnoreCase_FindsGames() {
            List<Game> found = gameRepository.findByDeveloperContainingIgnoreCase("Developer A");

            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("findByPublisherContainingIgnoreCase should find games by publisher")
        void findByPublisherContainingIgnoreCase_FindsGames() {
            List<Game> found = gameRepository.findByPublisherContainingIgnoreCase("Publisher A");

            assertThat(found).hasSize(1);
            assertThat(found.getFirst().getTitle()).isEqualTo("Alpha Game");
        }
    }

    @Nested
    @DisplayName("Status Operations")
    class StatusOperations {

        @Test
        @DisplayName("findByStatus should return games with specific status")
        void findByStatus_ReturnsGamesWithStatus() {
            List<Game> found = gameRepository.findByStatus(GameStatus.COMPLETED);

            assertThat(found).hasSize(1);
            assertThat(found.getFirst().getTitle()).isEqualTo("Alpha Game");
        }

        @Test
        @DisplayName("findByStatus should return empty for status with no games")
        void findByStatus_ReturnsEmptyForNoGames() {
            List<Game> found = gameRepository.findByStatus(GameStatus.DROPPED);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("countByStatus should return correct count")
        void countByStatus_ReturnsCorrectCount() {
            long completedCount = gameRepository.countByStatus(GameStatus.COMPLETED);
            long inProgressCount = gameRepository.countByStatus(GameStatus.IN_PROGRESS);
            long notStartedCount = gameRepository.countByStatus(GameStatus.NOT_STARTED);

            assertThat(completedCount).isEqualTo(1);
            assertThat(inProgressCount).isEqualTo(1);
            assertThat(notStartedCount).isEqualTo(1);
        }

        @Test
        @DisplayName("findByStatusOrderByCompletedAtDesc should return ordered games")
        void findByStatusOrderByCompletedAtDesc_ReturnsOrderedGames() {
            List<Game> found = gameRepository.findByStatusOrderByCompletedAtDesc(GameStatus.COMPLETED);

            assertThat(found).hasSize(1);
            assertThat(found.getFirst().getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Platform Operations")
    class PlatformOperations {

        @Test
        @DisplayName("findByPlatform should return games with specific platform")
        void findByPlatform_ReturnsGamesWithPlatform() {
            List<Game> found = gameRepository.findByPlatform(GamePlatform.PC);

            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("findByPlatform should return empty for platform with no games")
        void findByPlatform_ReturnsEmptyForNoGames() {
            List<Game> found = gameRepository.findByPlatform(GamePlatform.XBOX_SERIES);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("findByPlatform with pageable should return paged results")
        void findByPlatform_ReturnsPagedResults() {
            Page<Game> page = gameRepository.findByPlatform(GamePlatform.PC, PageRequest.of(0, 1));

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Favorites Operations")
    class FavoritesOperations {

        @Test
        @DisplayName("findByFavoriteTrue should return favorite games")
        void findByFavoriteTrue_ReturnsFavoriteGames() {
            List<Game> found = gameRepository.findByFavoriteTrue();

            assertThat(found).hasSize(1);
            assertThat(found.getFirst().getTitle()).isEqualTo("Alpha Game");
        }

        @Test
        @DisplayName("countByFavoriteTrue should return correct count")
        void countByFavoriteTrue_ReturnsCorrectCount() {
            long count = gameRepository.countByFavoriteTrue();

            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Statistics Operations")
    class StatisticsOperations {

        @Test
        @DisplayName("getTotalPlaytime should sum all playtime")
        void getTotalPlaytime_SumsAllPlaytime() {
            Double totalPlaytime = gameRepository.getTotalPlaytime();

            assertThat(totalPlaytime).isEqualTo(80.0); // 50 + 30 + 0
        }

        @Test
        @DisplayName("getAverageRating should calculate average of non-null ratings")
        void getAverageRating_CalculatesAverage() {
            Double averageRating = gameRepository.getAverageRating();

            // Only games with ratings: 9 and 8, average = 8.5
            assertThat(averageRating).isEqualTo(8.5);
        }

        @Test
        @DisplayName("getAverageRating should return 0 when no ratings")
        void getAverageRating_ReturnsZeroWhenNoRatings() {
            // Remove all games and add one without rating
            gameRepository.deleteAll();
            entityManager.flush();

            Game noRatingGame = Game.builder()
                    .title("No Rating Game")
                    .platform(GamePlatform.PC)
                    .status(GameStatus.NOT_STARTED)
                    .rating(null)
                    .build();
            entityManager.persist(noRatingGame);
            entityManager.flush();

            Double averageRating = gameRepository.getAverageRating();

            assertThat(averageRating).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Release Year Operations")
    class ReleaseYearOperations {

        @Test
        @DisplayName("findByReleaseYear should return games from specific year")
        void findByReleaseYear_ReturnsGamesFromYear() {
            List<Game> found = gameRepository.findByReleaseYear(2024);

            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("findByReleaseYear should return empty for year with no games")
        void findByReleaseYear_ReturnsEmptyForNoGames() {
            List<Game> found = gameRepository.findByReleaseYear(2020);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Ordering Operations")
    class OrderingOperations {

        @Test
        @DisplayName("findAllByOrderByTitleAsc should return games ordered by title")
        void findAllByOrderByTitleAsc_ReturnsOrderedGames() {
            List<Game> found = gameRepository.findAllByOrderByTitleAsc();

            assertThat(found).hasSize(3);
            assertThat(found.getFirst().getTitle()).isEqualTo("Alpha Game");
            assertThat(found.get(1).getTitle()).isEqualTo("Beta Game");
            assertThat(found.get(2).getTitle()).isEqualTo("Gamma Test");
        }

        @Test
        @DisplayName("findAllByOrderByUpdatedAtDesc should return games ordered by updated date")
        void findAllByOrderByUpdatedAtDesc_ReturnsOrderedGames() {
            // Update one game to make it most recent
            game2.setNotes("Updated notes");
            entityManager.persist(game2);
            entityManager.flush();

            List<Game> found = gameRepository.findAllByOrderByUpdatedAtDesc();

            assertThat(found).hasSize(3);
        }

        @Test
        @DisplayName("findTop10ByOrderByCreatedAtDesc should return most recent games")
        void findTop10ByOrderByCreatedAtDesc_ReturnsMostRecent() {
            List<Game> found = gameRepository.findTop10ByOrderByCreatedAtDesc();

            assertThat(found).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Pagination Operations")
    class PaginationOperations {

        @Test
        @DisplayName("findByStatus with pageable should return paged results")
        void findByStatus_WithPageable_ReturnsPagedResults() {
            // Add more completed games
            for (int i = 0; i < 5; i++) {
                Game game = Game.builder()
                        .title("Completed Game " + i)
                        .platform(GamePlatform.PC)
                        .status(GameStatus.COMPLETED)
                        .build();
                entityManager.persist(game);
            }
            entityManager.flush();

            Page<Game> page = gameRepository.findByStatus(
                    GameStatus.COMPLETED, PageRequest.of(0, 3));

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(6);
            assertThat(page.getTotalPages()).isEqualTo(2);
            assertThat(page.hasNext()).isTrue();
        }

        @Test
        @DisplayName("pagination should work correctly on second page")
        void pagination_WorksOnSecondPage() {
            // Add more games
            for (int i = 0; i < 5; i++) {
                Game game = Game.builder()
                        .title("Extra Game " + i)
                        .platform(GamePlatform.PC)
                        .status(GameStatus.NOT_STARTED)
                        .build();
                entityManager.persist(game);
            }
            entityManager.flush();

            Page<Game> page = gameRepository.findByStatus(
                    GameStatus.NOT_STARTED, PageRequest.of(1, 3));

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getNumber()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle games with empty collections")
        void shouldHandleGamesWithEmptyCollections() {
            Game gameWithEmptyCollections = Game.builder()
                    .title("Empty Collections Game")
                    .platform(GamePlatform.PC)
                    .status(GameStatus.NOT_STARTED)
                    .build();

            Game saved = gameRepository.save(gameWithEmptyCollections);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getGenres()).isEmpty();
            assertThat(saved.getPlatforms()).isEmpty();
        }

        @Test
        @DisplayName("should handle games with long description")
        void shouldHandleGamesWithLongDescription() {
            String longDescription = "A".repeat(10000);
            Game gameWithLongDescription = Game.builder()
                    .title("Long Description Game")
                    .description(longDescription)
                    .platform(GamePlatform.PC)
                    .status(GameStatus.NOT_STARTED)
                    .build();

            Game saved = gameRepository.save(gameWithLongDescription);

            assertThat(saved.getDescription()).hasSize(10000);
        }

        @Test
        @DisplayName("should handle special characters in title")
        void shouldHandleSpecialCharactersInTitle() {
            Game gameWithSpecialChars = Game.builder()
                    .title("Game: The 'Special' Edition - Part 1 & 2 (2024)")
                    .platform(GamePlatform.PC)
                    .status(GameStatus.NOT_STARTED)
                    .build();

            gameRepository.save(gameWithSpecialChars);

            List<Game> found = gameRepository.findByTitleContainingIgnoreCase("Special");
            assertThat(found).hasSize(1);
        }
    }
}
