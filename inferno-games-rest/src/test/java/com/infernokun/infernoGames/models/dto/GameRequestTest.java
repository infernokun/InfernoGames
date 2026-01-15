package com.infernokun.infernoGames.models.dto;

import com.infernokun.infernoGames.models.enums.GamePlatform;
import com.infernokun.infernoGames.models.enums.GameStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GameRequest DTO Tests")
class GameRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("builder should create request with all fields")
        void builder_CreatesRequestWithAllFields() {
            LocalDateTime now = LocalDateTime.now();
            
            GameRequest request = GameRequest.builder()
                    .title("Test Game")
                    .description("A test description")
                    .developer("Test Developer")
                    .publisher("Test Publisher")
                    .releaseYear(2024)
                    .genre("Action")
                    .genres(List.of("Action", "Adventure"))
                    .coverImageUrl("https://example.com/cover.jpg")
                    .screenshotUrls(List.of("https://example.com/ss1.jpg"))
                    .platform(GamePlatform.PC)
                    .platforms(List.of(GamePlatform.PC, GamePlatform.PLAYSTATION_5))
                    .status(GameStatus.IN_PROGRESS)
                    .rating(9)
                    .playtimeHours(50.0)
                    .completionPercentage(75)
                    .startedAt(now.minusDays(30))
                    .completedAt(null)
                    .notes("Great game!")
                    .favorite(true)
                    .achievements(25)
                    .totalAchievements(50)
                    .igdbId(12345L)
                    .build();

            assertThat(request.getTitle()).isEqualTo("Test Game");
            assertThat(request.getDescription()).isEqualTo("A test description");
            assertThat(request.getDeveloper()).isEqualTo("Test Developer");
            assertThat(request.getPublisher()).isEqualTo("Test Publisher");
            assertThat(request.getReleaseYear()).isEqualTo(2024);
            assertThat(request.getGenre()).isEqualTo("Action");
            assertThat(request.getGenres()).containsExactly("Action", "Adventure");
            assertThat(request.getCoverImageUrl()).isEqualTo("https://example.com/cover.jpg");
            assertThat(request.getScreenshotUrls()).containsExactly("https://example.com/ss1.jpg");
            assertThat(request.getPlatform()).isEqualTo(GamePlatform.PC);
            assertThat(request.getPlatforms()).containsExactly(GamePlatform.PC, GamePlatform.PLAYSTATION_5);
            assertThat(request.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(request.getRating()).isEqualTo(9);
            assertThat(request.getPlaytimeHours()).isEqualTo(50.0);
            assertThat(request.getCompletionPercentage()).isEqualTo(75);
            assertThat(request.getStartedAt()).isEqualTo(now.minusDays(30));
            assertThat(request.getCompletedAt()).isNull();
            assertThat(request.getNotes()).isEqualTo("Great game!");
            assertThat(request.getFavorite()).isTrue();
            assertThat(request.getAchievements()).isEqualTo(25);
            assertThat(request.getTotalAchievements()).isEqualTo(50);
            assertThat(request.getIgdbId()).isEqualTo(12345L);
        }

        @Test
        @DisplayName("builder should handle minimal request")
        void builder_HandlesMinimalRequest() {
            GameRequest request = GameRequest.builder()
                    .title("Minimal Game")
                    .build();

            assertThat(request.getTitle()).isEqualTo("Minimal Game");
            assertThat(request.getDescription()).isNull();
            assertThat(request.getRating()).isNull();
            assertThat(request.getStatus()).isNull();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("valid request should have no violations")
        void validRequest_HasNoViolations() {
            GameRequest request = GameRequest.builder()
                    .title("Valid Game")
                    .rating(8)
                    .completionPercentage(50)
                    .playtimeHours(10.0)
                    .build();

            Set<ConstraintViolation<GameRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("title exceeding max length should fail validation")
        void title_ExceedingMaxLength_FailsValidation() {
            GameRequest request = GameRequest.builder()
                    .title("A".repeat(256)) // Max is 255
                    .build();

            Set<ConstraintViolation<GameRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("must not exceed 255 characters");
        }

        @Test
        @DisplayName("title at max length should pass validation")
        void title_AtMaxLength_PassesValidation() {
            GameRequest request = GameRequest.builder()
                    .title("A".repeat(255))
                    .build();

            Set<ConstraintViolation<GameRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("rating below minimum should fail validation")
        void rating_BelowMinimum_FailsValidation() {
            GameRequest request = GameRequest.builder()
                    .title("Test Game")
                    .rating(0) // Min is 1
                    .build();

            Set<ConstraintViolation<GameRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("rating");
        }

        @Test
        @DisplayName("rating above maximum should fail validation")
        void rating_AboveMaximum_FailsValidation() {
            GameRequest request = GameRequest.builder()
                    .title("Test Game")
                    .rating(11) // Max is 10
                    .build();

            Set<ConstraintViolation<GameRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("rating");
        }

        @Test
        @DisplayName("valid ratings 1-10 should pass validation")
        void validRatings_PassValidation() {
            for (int rating = 1; rating <= 10; rating++) {
                GameRequest request = GameRequest.builder()
                        .title("Test Game")
                        .rating(rating)
                        .build();

                Set<ConstraintViolation<GameRequest>> violations = validator.validate(request);
                assertThat(violations).isEmpty();
            }
        }

        @Test
        @DisplayName("negative playtime should fail validation")
        void negativePlaytime_FailsValidation() {
            GameRequest request = GameRequest.builder()
                    .title("Test Game")
                    .playtimeHours(-5.0)
                    .build();

            Set<ConstraintViolation<GameRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("playtimeHours");
        }

        @Test
        @DisplayName("zero playtime should pass validation")
        void zeroPlaytime_PassesValidation() {
            GameRequest request = GameRequest.builder()
                    .title("Test Game")
                    .playtimeHours(0.0)
                    .build();

            Set<ConstraintViolation<GameRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("completion percentage below 0 should fail validation")
        void completionPercentage_BelowZero_FailsValidation() {
            GameRequest request = GameRequest.builder()
                    .title("Test Game")
                    .completionPercentage(-1)
                    .build();

            Set<ConstraintViolation<GameRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getPropertyPath().toString())
                    .isEqualTo("completionPercentage");
        }

        @Test
        @DisplayName("completion percentage above 100 should fail validation")
        void completionPercentage_AboveHundred_FailsValidation() {
            GameRequest request = GameRequest.builder()
                    .title("Test Game")
                    .completionPercentage(101)
                    .build();

            Set<ConstraintViolation<GameRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getPropertyPath().toString())
                    .isEqualTo("completionPercentage");
        }

        @Test
        @DisplayName("valid completion percentages 0-100 should pass validation")
        void validCompletionPercentages_PassValidation() {
            for (int percentage : new int[]{0, 25, 50, 75, 100}) {
                GameRequest request = GameRequest.builder()
                        .title("Test Game")
                        .completionPercentage(percentage)
                        .build();

                Set<ConstraintViolation<GameRequest>> violations = validator.validate(request);
                assertThat(violations).isEmpty();
            }
        }

        @Test
        @DisplayName("null optional fields should pass validation")
        void nullOptionalFields_PassValidation() {
            GameRequest request = GameRequest.builder()
                    .title("Test Game")
                    .description(null)
                    .rating(null)
                    .playtimeHours(null)
                    .completionPercentage(null)
                    .build();

            Set<ConstraintViolation<GameRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("setters should update fields")
        void setters_UpdateFields() {
            GameRequest request = new GameRequest();
            
            request.setTitle("Updated Title");
            request.setDescription("Updated Description");
            request.setRating(9);
            request.setStatus(GameStatus.COMPLETED);
            request.setFavorite(true);

            assertThat(request.getTitle()).isEqualTo("Updated Title");
            assertThat(request.getDescription()).isEqualTo("Updated Description");
            assertThat(request.getRating()).isEqualTo(9);
            assertThat(request.getStatus()).isEqualTo(GameStatus.COMPLETED);
            assertThat(request.getFavorite()).isTrue();
        }
    }

    @Nested
    @DisplayName("No Args Constructor Tests")
    class NoArgsConstructorTests {

        @Test
        @DisplayName("no args constructor should create empty request")
        void noArgsConstructor_CreatesEmptyRequest() {
            GameRequest request = new GameRequest();

            assertThat(request.getTitle()).isNull();
            assertThat(request.getDescription()).isNull();
            assertThat(request.getRating()).isNull();
            assertThat(request.getStatus()).isNull();
        }
    }

    @Nested
    @DisplayName("All Args Constructor Tests")
    class AllArgsConstructorTests {

        @Test
        @DisplayName("all args constructor should set all fields")
        void allArgsConstructor_SetsAllFields() {
            GameRequest request = GameRequest.builder()
                    .title("Title")
                    .description("Description")
                    .developer("Developer")
                    .publisher("Publisher")
                    .releaseYear(2024)
                    .genre("Action")
                    .genres(List.of("Action"))
                    .coverImageUrl("https://cover.jpg")
                    .screenshotUrls(List.of("https://ss.jpg"))
                    .platform(GamePlatform.PC)
                    .status(GameStatus.IN_PROGRESS)
                    .rating(8)
                    .playtimeHours(15.5)
                    .completionPercentage(75)
                    .startedAt(LocalDateTime.now().minusDays(1))
                    .notes("Notes")
                    .favorite(true)
                    .achievements(25)
                    .totalAchievements(50)
                    .steamAppId("12345")
                    .igdbId(98765L)
                    .build();

            assertThat(request.getTitle()).isEqualTo("Title");
            assertThat(request.getDescription()).isEqualTo("Description");
            assertThat(request.getDeveloper()).isEqualTo("Developer");
            assertThat(request.getPublisher()).isEqualTo("Publisher");
            assertThat(request.getReleaseYear()).isEqualTo(2024);
            assertThat(request.getGenre()).isEqualTo("Action");
            assertThat(request.getGenres()).containsExactlyInAnyOrder("Action");
            assertThat(request.getPlatform()).isEqualTo(GamePlatform.PC);
            assertThat(request.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(request.getRating()).isEqualTo(8);
            assertThat(request.getPlaytimeHours()).isEqualTo(15.5);
            assertThat(request.getCompletionPercentage()).isEqualTo(75);
            assertThat(request.getStartedAt()).isNotNull();
            assertThat(request.getNotes()).isEqualTo("Notes");
            assertThat(request.getFavorite()).isTrue();
            assertThat(request.getAchievements()).isEqualTo(25);
            assertThat(request.getTotalAchievements()).isEqualTo(50);
            assertThat(request.getSteamAppId()).isEqualTo("12345");
            assertThat(request.getIgdbId()).isEqualTo(98765L);
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("equal requests should be equal")
        void equalRequests_ShouldBeEqual() {
            GameRequest request1 = GameRequest.builder()
                    .title("Test Game")
                    .rating(8)
                    .build();

            GameRequest request2 = GameRequest.builder()
                    .title("Test Game")
                    .rating(8)
                    .build();

            assertThat(request1).isEqualTo(request2);
            assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        }

        @Test
        @DisplayName("different requests should not be equal")
        void differentRequests_ShouldNotBeEqual() {
            GameRequest request1 = GameRequest.builder()
                    .title("Test Game 1")
                    .build();

            GameRequest request2 = GameRequest.builder()
                    .title("Test Game 2")
                    .build();

            assertThat(request1).isNotEqualTo(request2);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString should contain field values")
        void toString_ContainsFieldValues() {
            GameRequest request = GameRequest.builder()
                    .title("Test Game")
                    .rating(8)
                    .build();

            String result = request.toString();

            assertThat(result).contains("Test Game");
            assertThat(result).contains("8");
        }
    }
}
