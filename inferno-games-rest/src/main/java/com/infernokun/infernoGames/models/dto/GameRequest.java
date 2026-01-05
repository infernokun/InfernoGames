package com.infernokun.infernoGames.models.dto;

import com.infernokun.infernoGames.models.enums.GamePlatform;
import com.infernokun.infernoGames.models.enums.GameStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameRequest {

    @Size(max = 255, message = "Game title must not exceed 255 characters")
    private String title;

    private String description;

    private String developer;

    private String publisher;

    private Integer releaseYear;

    private String genre;

    private List<String> genres;

    private String coverImageUrl;

    private List<String> screenshotUrls;

    private GamePlatform platform;

    private List<GamePlatform> platforms;

    private GameStatus status;

    @Min(1)
    @Max(10)
    private Integer rating;

    @Min(0)
    private Double playtimeHours;

    @Min(0)
    @Max(100)
    private Integer completionPercentage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String notes;

    private Boolean favorite;

    private Integer achievements;

    private Integer totalAchievements;

    // IGDB fields
    private Long igdbId;
}
