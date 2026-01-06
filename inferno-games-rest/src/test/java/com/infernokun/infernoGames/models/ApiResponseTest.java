package com.infernokun.infernoGames.models;

import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ApiResponse Tests")
class ApiResponseTest {

    @Nested
    @DisplayName("Static Factory Methods")
    class StaticFactoryMethods {

        @Test
        @DisplayName("success() should create success response without data")
        void success_CreatesSuccessResponseWithoutData() {
            ApiResponse<Void> response = ApiResponse.success();

            assertThat(response.getCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(response.getMessage()).isEqualTo("Success");
            assertThat(response.getType()).isEqualTo(ApiResponse.TYPE.SUCCESS);
            assertThat(response.getData()).isNull();
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("success(data) should create success response with data")
        void success_CreatesSuccessResponseWithData() {
            String testData = "test data";
            ApiResponse<String> response = ApiResponse.success(testData);

            assertThat(response.getCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(response.getMessage()).isEqualTo("Success");
            assertThat(response.getType()).isEqualTo(ApiResponse.TYPE.SUCCESS);
            assertThat(response.getData()).isEqualTo(testData);
        }

        @Test
        @DisplayName("success(message) should create success response with message")
        void success_CreatesSuccessResponseWithMessage() {
            ApiResponse<Void> response = ApiResponse.success("Custom message");

            assertThat(response.getCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(response.getMessage()).isEqualTo("Custom message");
            assertThat(response.getType()).isEqualTo(ApiResponse.TYPE.SUCCESS);
        }

        @Test
        @DisplayName("success(data, message) should create success response with data and message")
        void success_CreatesSuccessResponseWithDataAndMessage() {
            String testData = "test data";
            ApiResponse<String> response = ApiResponse.success(testData, "Custom message");

            assertThat(response.getCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(response.getMessage()).isEqualTo("Custom message");
            assertThat(response.getData()).isEqualTo(testData);
            assertThat(response.getType()).isEqualTo(ApiResponse.TYPE.SUCCESS);
        }

        @Test
        @DisplayName("success with pagination should include pagination info")
        void success_WithPagination() {
            String testData = "test data";
            ApiResponse<String> response = ApiResponse.success(testData, "Message", 100, 1, 10);

            assertThat(response.getTotalCount()).isEqualTo(100);
            assertThat(response.getCurrentPage()).isEqualTo(1);
            assertThat(response.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("error(message) should create error response")
        void error_CreatesErrorResponse() {
            ApiResponse<Void> response = ApiResponse.error("Error occurred");

            assertThat(response.getCode()).isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            assertThat(response.getMessage()).isEqualTo("Error occurred");
            assertThat(response.getType()).isEqualTo(ApiResponse.TYPE.ERROR);
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("error(message, code) should create error response with custom code")
        void error_CreatesErrorResponseWithCustomCode() {
            ApiResponse<Void> response = ApiResponse.error("Not found", HttpStatus.SC_NOT_FOUND);

            assertThat(response.getCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo("Not found");
            assertThat(response.getType()).isEqualTo(ApiResponse.TYPE.ERROR);
        }

        @Test
        @DisplayName("warning(message) should create warning response")
        void warning_CreatesWarningResponse() {
            ApiResponse<Void> response = ApiResponse.warning("Warning message");

            assertThat(response.getCode()).isEqualTo(HttpStatus.SC_ACCEPTED);
            assertThat(response.getMessage()).isEqualTo("Warning message");
            assertThat(response.getType()).isEqualTo(ApiResponse.TYPE.WARNING);
        }

        @Test
        @DisplayName("info(message) should create info response")
        void info_CreatesInfoResponse() {
            ApiResponse<Void> response = ApiResponse.info("Info message");

            assertThat(response.getCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(response.getMessage()).isEqualTo("Info message");
            assertThat(response.getType()).isEqualTo(ApiResponse.TYPE.INFO);
        }

        @Test
        @DisplayName("notFound(message) should create not found response")
        void notFound_CreatesNotFoundResponse() {
            ApiResponse<Void> response = ApiResponse.notFound("Resource not found");

            assertThat(response.getCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo("Resource not found");
            assertThat(response.getType()).isEqualTo(ApiResponse.TYPE.INFO);
        }
    }

    @Nested
    @DisplayName("Helper Methods")
    class HelperMethods {

        @Test
        @DisplayName("isSuccess should return true for success response")
        void isSuccess_ReturnsTrueForSuccess() {
            ApiResponse<Void> response = ApiResponse.success();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.isError()).isFalse();
            assertThat(response.isWarning()).isFalse();
        }

        @Test
        @DisplayName("isError should return true for error response")
        void isError_ReturnsTrueForError() {
            ApiResponse<Void> response = ApiResponse.error("Error");

            assertThat(response.isError()).isTrue();
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.isWarning()).isFalse();
        }

        @Test
        @DisplayName("isWarning should return true for warning response")
        void isWarning_ReturnsTrueForWarning() {
            ApiResponse<Void> response = ApiResponse.warning("Warning");

            assertThat(response.isWarning()).isTrue();
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.isError()).isFalse();
        }

        @Test
        @DisplayName("hasData should return true when data is present")
        void hasData_ReturnsTrueWhenDataPresent() {
            ApiResponse<String> response = ApiResponse.success("data");

            assertThat(response.hasData()).isTrue();
        }

        @Test
        @DisplayName("hasData should return false when data is null")
        void hasData_ReturnsFalseWhenDataNull() {
            ApiResponse<Void> response = ApiResponse.success();

            assertThat(response.hasData()).isFalse();
        }

        @Test
        @DisplayName("withPagination should add pagination info")
        void withPagination_AddsPaginationInfo() {
            ApiResponse<String> response = ApiResponse.success("data");
            response.withPagination(100, 2, 20);

            assertThat(response.getTotalCount()).isEqualTo(100);
            assertThat(response.getCurrentPage()).isEqualTo(2);
            assertThat(response.getPageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("withPagination should return same response for chaining")
        void withPagination_ReturnsSameResponseForChaining() {
            ApiResponse<String> response = ApiResponse.success("data");
            ApiResponse<String> result = response.withPagination(100, 2, 20);

            assertThat(result).isSameAs(response);
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("builder should create response with all fields")
        void builder_CreatesResponseWithAllFields() {
            LocalDateTime now = LocalDateTime.now();
            
            ApiResponse<String> response = ApiResponse.<String>builder()
                    .code(HttpStatus.SC_OK)
                    .message("Test message")
                    .data("test data")
                    .type(ApiResponse.TYPE.SUCCESS)
                    .timeMs(100L)
                    .timestamp(now)
                    .totalCount(50)
                    .currentPage(1)
                    .pageSize(10)
                    .build();

            assertThat(response.getCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(response.getMessage()).isEqualTo("Test message");
            assertThat(response.getData()).isEqualTo("test data");
            assertThat(response.getType()).isEqualTo(ApiResponse.TYPE.SUCCESS);
            assertThat(response.getTimeMs()).isEqualTo(100L);
            assertThat(response.getTimestamp()).isEqualTo(now);
            assertThat(response.getTotalCount()).isEqualTo(50);
            assertThat(response.getCurrentPage()).isEqualTo(1);
            assertThat(response.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("builder should set default code to OK")
        void builder_SetsDefaultCodeToOk() {
            ApiResponse<Void> response = ApiResponse.<Void>builder()
                    .message("Test")
                    .build();

            assertThat(response.getCode()).isEqualTo(HttpStatus.SC_OK);
        }
    }

    @Nested
    @DisplayName("No Args Constructor Tests")
    class NoArgsConstructorTests {

        @Test
        @DisplayName("no args constructor should create empty response")
        void noArgsConstructor_CreatesEmptyResponse() {
            ApiResponse<String> response = new ApiResponse<>();

            assertThat(response.getData()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getType()).isNull();
        }
    }

    @Nested
    @DisplayName("All Args Constructor Tests")
    class AllArgsConstructorTests {

        @Test
        @DisplayName("all args constructor should set all fields")
        void allArgsConstructor_SetsAllFields() {
            LocalDateTime now = LocalDateTime.now();
            
            ApiResponse<String> response = new ApiResponse<>(
                    200,
                    "Message",
                    "Data",
                    ApiResponse.TYPE.SUCCESS,
                    100L,
                    now,
                    50,
                    1,
                    10
            );

            assertThat(response.getCode()).isEqualTo(200);
            assertThat(response.getMessage()).isEqualTo("Message");
            assertThat(response.getData()).isEqualTo("Data");
            assertThat(response.getType()).isEqualTo(ApiResponse.TYPE.SUCCESS);
            assertThat(response.getTimeMs()).isEqualTo(100L);
            assertThat(response.getTimestamp()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("Type Enum Tests")
    class TypeEnumTests {

        @Test
        @DisplayName("TYPE enum should have all expected values")
        void typeEnum_HasAllExpectedValues() {
            ApiResponse.TYPE[] types = ApiResponse.TYPE.values();

            assertThat(types).containsExactlyInAnyOrder(
                    ApiResponse.TYPE.INFO,
                    ApiResponse.TYPE.WARNING,
                    ApiResponse.TYPE.ERROR,
                    ApiResponse.TYPE.SUCCESS,
                    ApiResponse.TYPE.NONE
            );
        }
    }

    @Nested
    @DisplayName("Timestamp Tests")
    class TimestampTests {

        @Test
        @DisplayName("timestamp should be set automatically")
        void timestamp_SetAutomatically() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            ApiResponse<Void> response = ApiResponse.success();
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            assertThat(response.getTimestamp()).isAfter(before);
            assertThat(response.getTimestamp()).isBefore(after);
        }
    }

    @Nested
    @DisplayName("Generic Type Tests")
    class GenericTypeTests {

        @Test
        @DisplayName("should work with different data types")
        void shouldWorkWithDifferentDataTypes() {
            ApiResponse<Integer> intResponse = ApiResponse.success(42);
            ApiResponse<Double> doubleResponse = ApiResponse.success(3.14);
            ApiResponse<Boolean> boolResponse = ApiResponse.success(true);

            assertThat(intResponse.getData()).isEqualTo(42);
            assertThat(doubleResponse.getData()).isEqualTo(3.14);
            assertThat(boolResponse.getData()).isTrue();
        }

        @Test
        @DisplayName("should work with complex types")
        void shouldWorkWithComplexTypes() {
            Game game = Game.builder().id(1L).title("Test Game").build();
            ApiResponse<Game> response = ApiResponse.success(game);

            assertThat(response.getData()).isEqualTo(game);
            assertThat(response.getData().getTitle()).isEqualTo("Test Game");
        }
    }
}
