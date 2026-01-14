package com.infernokun.infernoGames.models;

import com.infernokun.infernoGames.models.enums.GamePlatform;
import com.infernokun.infernoGames.models.enums.GameStatus;
import com.infernokun.infernoGames.utils.GamePlatformListConverter;
import com.infernokun.infernoGames.utils.StringListConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Game title is required")
    @Size(max = 255, message = "Game title must not exceed 255 characters")
    @Column(nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "developer")
    private String developer;

    @Column(name = "publisher")
    private String publisher;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "release_date")
    private LocalDateTime releaseDate;

    @Column(name = "genre")
    private String genre;

    @Builder.Default
    @Column(name = "genres", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> genres = new ArrayList<>();

    @Column(name = "cover_image_url", length = 1024)
    private String coverImageUrl;

    @Builder.Default
    @Column(name = "screenshot_urls", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> screenshotUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "platform")
    private GamePlatform platform;

    @Builder.Default
    @Column(name = "platforms", columnDefinition = "TEXT")
    @Convert(converter = GamePlatformListConverter.class)
    private List<GamePlatform> platforms = new ArrayList<>();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private GameStatus status = GameStatus.NOT_STARTED;

    @Min(1)
    @Max(10)
    @Column(name = "rating")
    private Integer rating;

    @Column(name = "playtime_hours")
    @Builder.Default
    private Double playtimeHours = 0.0;

    @Min(0)
    @Max(100)
    @Column(name = "completion_percentage")
    @Builder.Default
    private Integer completionPercentage = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "favorite")
    private Boolean favorite = false;

    @Builder.Default
    @Column(name = "is_dlc")
    private Boolean isDlc = false;

    @Builder.Default
    @Column(name = "achievements")
    private Integer achievements = 0;

    @Builder.Default
    @Column(name = "total_achievements")
    private Integer totalAchievements = 0;

    // IGDB Integration fields
    @Column(name = "igdb_id")
    private Long igdbId;

    @Column(name = "igdb_url", length = 1024)
    private String igdbUrl;

    @Column(name = "igdb_rating")
    private Double igdbRating;

    @Column(name = "igdb_rating_count")
    private Integer igdbRatingCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "steam_app_id")
    private String steamAppId;

    // Steam-specific playtime tracking
    @Column(name = "steam_playtime_windows_minutes")
    private Integer steamPlaytimeWindowsMinutes;

    @Column(name = "steam_playtime_linux_minutes")
    private Integer steamPlaytimeLinuxMinutes;

    @Column(name = "steam_playtime_mac_minutes")
    private Integer steamPlaytimeMacMinutes;

    @Column(name = "steam_playtime_deck_minutes")
    private Integer steamPlaytimeDeckMinutes;

    @Column(name = "steam_last_played")
    private LocalDateTime steamLastPlayed;

    @Column(name = "steam_last_synced")
    private LocalDateTime steamLastSynced;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to update status timestamps
    public void updateStatus(GameStatus newStatus) {
        this.status = newStatus;
        if (newStatus == GameStatus.IN_PROGRESS && this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        } else if (newStatus == GameStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
            this.completionPercentage = 100;
        }
    }
}