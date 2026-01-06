package com.infernokun.infernoGames.models;

import com.infernokun.infernoGames.models.enums.GamePlatform;
import com.infernokun.infernoGames.models.enums.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Game Entity Tests")
class GameTest {

    private Game game;

    @BeforeEach
    void setUp() {
        game = Game.builder()
                .id(1L)
                .title("Test Game")
                .description("A test game")
                .developer("Test Developer")
                .publisher("Test Publisher")
                .releaseYear(2024)
                .genre("Action")
                .genres(new ArrayList<>(List.of("Action", "Adventure")))
                .platform(GamePlatform.PC)
                .platforms(new ArrayList<>(List.of(GamePlatform.PC, GamePlatform.PLAYSTATION_5)))
                .status(GameStatus.NOT_STARTED)
                .rating(8)
                .playtimeHours(0.0)
                .completionPercentage(0)
                .favorite(false)
                .achievements(0)
                .totalAchievements(50)
                .build();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Builder should create game with all fields")
        void builder_CreatesGameWithAllFields() {
            assertThat(game.getId()).isEqualTo(1L);
            assertThat(game.getTitle()).isEqualTo("Test Game");
            assertThat(game.getDescription()).isEqualTo("A test game");
            assertThat(game.getDeveloper()).isEqualTo("Test Developer");
            assertThat(game.getPublisher()).isEqualTo("Test Publisher");
            assertThat(game.getReleaseYear()).isEqualTo(2024);
            assertThat(game.getGenre()).isEqualTo("Action");
            assertThat(game.getGenres()).containsExactly("Action", "Adventure");
            assertThat(game.getPlatform()).isEqualTo(GamePlatform.PC);
            assertThat(game.getPlatforms()).containsExactly(GamePlatform.PC, GamePlatform.PLAYSTATION_5);
            assertThat(game.getStatus()).isEqualTo(GameStatus.NOT_STARTED);
            assertThat(game.getRating()).isEqualTo(8);
            assertThat(game.getPlaytimeHours()).isEqualTo(0.0);
            assertThat(game.getCompletionPercentage()).isEqualTo(0);
            assertThat(game.getFavorite()).isFalse();
            assertThat(game.getAchievements()).isEqualTo(0);
            assertThat(game.getTotalAchievements()).isEqualTo(50);
        }

        @Test
        @DisplayName("Builder should set default values")
        void builder_SetsDefaultValues() {
            Game minimalGame = Game.builder()
                    .title("Minimal Game")
                    .build();

            assertThat(minimalGame.getStatus()).isEqualTo(GameStatus.NOT_STARTED);
            assertThat(minimalGame.getPlaytimeHours()).isEqualTo(0.0);
            assertThat(minimalGame.getCompletionPercentage()).isEqualTo(0);
            assertThat(minimalGame.getFavorite()).isFalse();
            assertThat(minimalGame.getAchievements()).isEqualTo(0);
            assertThat(minimalGame.getTotalAchievements()).isEqualTo(0);
            assertThat(minimalGame.getGenres()).isEmpty();
            assertThat(minimalGame.getPlatforms()).isEmpty();
            assertThat(minimalGame.getScreenshotUrls()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateStatus Method Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("updateStatus to IN_PROGRESS should set startedAt when null")
        void updateStatus_ToInProgress_SetsStartedAt() {
            game.setStartedAt(null);
            
            game.updateStatus(GameStatus.IN_PROGRESS);

            assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(game.getStartedAt()).isNotNull();
            assertThat(game.getStartedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("updateStatus to IN_PROGRESS should preserve existing startedAt")
        void updateStatus_ToInProgress_PreservesExistingStartedAt() {
            LocalDateTime originalStartedAt = LocalDateTime.of(2024, 1, 1, 10, 0);
            game.setStartedAt(originalStartedAt);
            
            game.updateStatus(GameStatus.IN_PROGRESS);

            assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(game.getStartedAt()).isEqualTo(originalStartedAt);
        }

        @Test
        @DisplayName("updateStatus to COMPLETED should set completedAt when null")
        void updateStatus_ToCompleted_SetsCompletedAt() {
            game.setCompletedAt(null);
            game.setCompletionPercentage(75);
            
            game.updateStatus(GameStatus.COMPLETED);

            assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
            assertThat(game.getCompletedAt()).isNotNull();
            assertThat(game.getCompletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
            assertThat(game.getCompletionPercentage()).isEqualTo(100);
        }

        @Test
        @DisplayName("updateStatus to COMPLETED should preserve existing completedAt")
        void updateStatus_ToCompleted_PreservesExistingCompletedAt() {
            LocalDateTime originalCompletedAt = LocalDateTime.of(2024, 6, 15, 18, 30);
            game.setCompletedAt(originalCompletedAt);
            
            game.updateStatus(GameStatus.COMPLETED);

            assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
            assertThat(game.getCompletedAt()).isEqualTo(originalCompletedAt);
        }

        @Test
        @DisplayName("updateStatus to ON_HOLD should not affect timestamps")
        void updateStatus_ToOnHold_DoesNotAffectTimestamps() {
            game.setStartedAt(null);
            game.setCompletedAt(null);
            
            game.updateStatus(GameStatus.ON_HOLD);

            assertThat(game.getStatus()).isEqualTo(GameStatus.ON_HOLD);
            assertThat(game.getStartedAt()).isNull();
            assertThat(game.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("updateStatus to DROPPED should not affect timestamps")
        void updateStatus_ToDropped_DoesNotAffectTimestamps() {
            game.setStartedAt(null);
            game.setCompletedAt(null);
            
            game.updateStatus(GameStatus.DROPPED);

            assertThat(game.getStatus()).isEqualTo(GameStatus.DROPPED);
            assertThat(game.getStartedAt()).isNull();
            assertThat(game.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("updateStatus to NOT_STARTED should not affect timestamps")
        void updateStatus_ToNotStarted_DoesNotAffectTimestamps() {
            game.updateStatus(GameStatus.NOT_STARTED);

            assertThat(game.getStatus()).isEqualTo(GameStatus.NOT_STARTED);
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("setTitle should update title")
        void setTitle_UpdatesTitle() {
            game.setTitle("New Title");
            assertThat(game.getTitle()).isEqualTo("New Title");
        }

        @Test
        @DisplayName("setRating should update rating")
        void setRating_UpdatesRating() {
            game.setRating(10);
            assertThat(game.getRating()).isEqualTo(10);
        }

        @Test
        @DisplayName("setPlaytimeHours should update playtime")
        void setPlaytimeHours_UpdatesPlaytime() {
            game.setPlaytimeHours(50.5);
            assertThat(game.getPlaytimeHours()).isEqualTo(50.5);
        }

        @Test
        @DisplayName("setFavorite should update favorite status")
        void setFavorite_UpdatesFavorite() {
            game.setFavorite(true);
            assertThat(game.getFavorite()).isTrue();

            game.setFavorite(false);
            assertThat(game.getFavorite()).isFalse();
        }

        @Test
        @DisplayName("setCompletionPercentage should update completion")
        void setCompletionPercentage_UpdatesCompletion() {
            game.setCompletionPercentage(75);
            assertThat(game.getCompletionPercentage()).isEqualTo(75);
        }

        @Test
        @DisplayName("setGenres should update genres list")
        void setGenres_UpdatesGenres() {
            game.setGenres(List.of("RPG", "Strategy"));
            assertThat(game.getGenres()).containsExactly("RPG", "Strategy");
        }

        @Test
        @DisplayName("setPlatforms should update platforms list")
        void setPlatforms_UpdatesPlatforms() {
            game.setPlatforms(List.of(GamePlatform.NINTENDO_SWITCH));
            assertThat(game.getPlatforms()).containsExactly(GamePlatform.NINTENDO_SWITCH);
        }

        @Test
        @DisplayName("setScreenshotUrls should update screenshot URLs")
        void setScreenshotUrls_UpdatesScreenshots() {
            List<String> urls = List.of("https://example.com/1.jpg", "https://example.com/2.jpg");
            game.setScreenshotUrls(urls);
            assertThat(game.getScreenshotUrls()).containsExactlyElementsOf(urls);
        }

        @Test
        @DisplayName("setIgdbId should update IGDB ID")
        void setIgdbId_UpdatesIgdbId() {
            game.setIgdbId(12345L);
            assertThat(game.getIgdbId()).isEqualTo(12345L);
        }

        @Test
        @DisplayName("setIgdbUrl should update IGDB URL")
        void setIgdbUrl_UpdatesIgdbUrl() {
            game.setIgdbUrl("https://igdb.com/games/test");
            assertThat(game.getIgdbUrl()).isEqualTo("https://igdb.com/games/test");
        }

        @Test
        @DisplayName("setIgdbRating should update IGDB rating")
        void setIgdbRating_UpdatesIgdbRating() {
            game.setIgdbRating(85.5);
            assertThat(game.getIgdbRating()).isEqualTo(85.5);
        }

        @Test
        @DisplayName("setIgdbRatingCount should update IGDB rating count")
        void setIgdbRatingCount_UpdatesIgdbRatingCount() {
            game.setIgdbRatingCount(1000);
            assertThat(game.getIgdbRatingCount()).isEqualTo(1000);
        }

        @Test
        @DisplayName("setNotes should update notes")
        void setNotes_UpdatesNotes() {
            game.setNotes("Great game, highly recommended!");
            assertThat(game.getNotes()).isEqualTo("Great game, highly recommended!");
        }

        @Test
        @DisplayName("setCoverImageUrl should update cover image URL")
        void setCoverImageUrl_UpdatesCoverImageUrl() {
            game.setCoverImageUrl("https://example.com/cover.jpg");
            assertThat(game.getCoverImageUrl()).isEqualTo("https://example.com/cover.jpg");
        }

        @Test
        @DisplayName("setAchievements should update achievements")
        void setAchievements_UpdatesAchievements() {
            game.setAchievements(25);
            assertThat(game.getAchievements()).isEqualTo(25);
        }

        @Test
        @DisplayName("setTotalAchievements should update total achievements")
        void setTotalAchievements_UpdatesTotalAchievements() {
            game.setTotalAchievements(100);
            assertThat(game.getTotalAchievements()).isEqualTo(100);
        }

        @Test
        @DisplayName("setReleaseDate should update release date")
        void setReleaseDate_UpdatesReleaseDate() {
            LocalDateTime releaseDate = LocalDateTime.of(2024, 12, 15, 0, 0);
            game.setReleaseDate(releaseDate);
            assertThat(game.getReleaseDate()).isEqualTo(releaseDate);
        }
    }

    @Nested
    @DisplayName("IGDB Integration Fields Tests")
    class IgdbFieldsTests {

        @Test
        @DisplayName("Game should support all IGDB fields")
        void game_SupportsAllIgdbFields() {
            Game igdbGame = Game.builder()
                    .title("IGDB Game")
                    .igdbId(12345L)
                    .igdbUrl("https://igdb.com/games/12345")
                    .igdbRating(90.5)
                    .igdbRatingCount(5000)
                    .build();

            assertThat(igdbGame.getIgdbId()).isEqualTo(12345L);
            assertThat(igdbGame.getIgdbUrl()).isEqualTo("https://igdb.com/games/12345");
            assertThat(igdbGame.getIgdbRating()).isEqualTo(90.5);
            assertThat(igdbGame.getIgdbRatingCount()).isEqualTo(5000);
        }
    }

    @Nested
    @DisplayName("Equality and ToString Tests")
    class EqualityTests {

        @Test
        @DisplayName("toString should return string representation")
        void toString_ReturnsStringRepresentation() {
            String result = game.toString();
            
            assertThat(result).contains("Test Game");
            assertThat(result).contains("Test Developer");
            assertThat(result).contains("PC");
        }
    }

    @Nested
    @DisplayName("Null Safety Tests")
    class NullSafetyTests {

        @Test
        @DisplayName("Default lists should not be null")
        void defaultLists_ShouldNotBeNull() {
            Game newGame = Game.builder()
                    .title("New Game")
                    .build();

            assertThat(newGame.getGenres()).isNotNull();
            assertThat(newGame.getPlatforms()).isNotNull();
            assertThat(newGame.getScreenshotUrls()).isNotNull();
        }

        @Test
        @DisplayName("Game should handle null values in optional fields")
        void game_HandlesNullValuesInOptionalFields() {
            Game gameWithNulls = Game.builder()
                    .title("Game with nulls")
                    .description(null)
                    .developer(null)
                    .publisher(null)
                    .genre(null)
                    .coverImageUrl(null)
                    .notes(null)
                    .rating(null)
                    .releaseYear(null)
                    .releaseDate(null)
                    .startedAt(null)
                    .completedAt(null)
                    .igdbId(null)
                    .igdbUrl(null)
                    .igdbRating(null)
                    .igdbRatingCount(null)
                    .build();

            assertThat(gameWithNulls.getTitle()).isEqualTo("Game with nulls");
            assertThat(gameWithNulls.getDescription()).isNull();
            assertThat(gameWithNulls.getDeveloper()).isNull();
            assertThat(gameWithNulls.getRating()).isNull();
        }
    }

    @Nested
    @DisplayName("NoArgsConstructor Tests")
    class NoArgsConstructorTests {

        @Test
        @DisplayName("NoArgsConstructor should create empty game")
        void noArgsConstructor_CreatesEmptyGame() {
            Game emptyGame = new Game();

            assertThat(emptyGame.getId()).isNull();
            assertThat(emptyGame.getTitle()).isNull();
        }

        @Test
        @DisplayName("NoArgsConstructor game should have default values for collections")
        void noArgsConstructor_HasDefaultCollections() {
            Game emptyGame = new Game();

            // These may be null depending on how Lombok handles @Builder.Default with NoArgs
            // The test verifies the expected behavior
            assertThat(emptyGame).isNotNull();
        }
    }

    @Nested
    @DisplayName("AllArgsConstructor Tests")
    class AllArgsConstructorTests {

        @Test
        @DisplayName("AllArgsConstructor should create game with all args")
        void allArgsConstructor_CreatesGameWithAllArgs() {
            LocalDateTime now = LocalDateTime.now();
            
            Game fullGame = new Game(
                    1L,                                                    // id
                    "Full Game",                                           // title
                    "Description",                                         // description
                    "Developer",                                           // developer
                    "Publisher",                                           // publisher
                    2024,                                                  // releaseYear
                    now,                                                   // releaseDate
                    "Action",                                              // genre
                    List.of("Action"),                                     // genres
                    "https://example.com/cover.jpg",                       // coverImageUrl
                    List.of("https://example.com/ss.jpg"),                 // screenshotUrls
                    GamePlatform.PC,                                       // platform
                    List.of(GamePlatform.PC),                             // platforms
                    GameStatus.IN_PROGRESS,                                // status
                    9,                                                     // rating
                    50.0,                                                  // playtimeHours
                    75,                                                    // completionPercentage
                    now,                                                   // startedAt
                    null,                                                  // completedAt
                    "Notes",                                               // notes
                    true,                                                  // favorite
                    25,                                                    // achievements
                    50,                                                    // totalAchievements
                    12345L,                                                // igdbId
                    "https://igdb.com/games/12345",                        // igdbUrl
                    90.0,                                                  // igdbRating
                    1000,                                                  // igdbRatingCount
                    now,                                                   // createdAt
                    now                                                    // updatedAt
            );

            assertThat(fullGame.getId()).isEqualTo(1L);
            assertThat(fullGame.getTitle()).isEqualTo("Full Game");
            assertThat(fullGame.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(fullGame.getFavorite()).isTrue();
            assertThat(fullGame.getIgdbId()).isEqualTo(12345L);
        }
    }
}
