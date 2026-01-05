package com.infernokun.infernoGames.repositories;

import com.infernokun.infernoGames.models.Game;
import com.infernokun.infernoGames.models.enums.GamePlatform;
import com.infernokun.infernoGames.models.enums.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    // Find by IGDB ID
    Optional<Game> findByIgdbId(Long igdbId);

    // Search by title
    List<Game> findByTitleContainingIgnoreCase(String title);

    // Find by status
    List<Game> findByStatus(GameStatus status);

    // Find by platform
    List<Game> findByPlatform(GamePlatform platform);

    // Find favorites
    List<Game> findByFavoriteTrue();

    // Find by genre
    List<Game> findByGenreContainingIgnoreCase(String genre);

    // Find by developer
    List<Game> findByDeveloperContainingIgnoreCase(String developer);

    // Find by publisher
    List<Game> findByPublisherContainingIgnoreCase(String publisher);

    // Complex search query
    @Query("SELECT g FROM Game g WHERE " +
            "(:title IS NULL OR LOWER(g.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:status IS NULL OR g.status = :status) AND " +
            "(:platform IS NULL OR g.platform = :platform) AND " +
            "(:genre IS NULL OR LOWER(g.genre) LIKE LOWER(CONCAT('%', :genre, '%')))")
    List<Game> searchGames(
            @Param("title") String title,
            @Param("status") GameStatus status,
            @Param("platform") GamePlatform platform,
            @Param("genre") String genre
    );

    // Get games ordered by most recently updated
    List<Game> findAllByOrderByUpdatedAtDesc();

    // Get games ordered by title
    List<Game> findAllByOrderByTitleAsc();

    // Count by status
    long countByStatus(GameStatus status);

    // Count favorites
    long countByFavoriteTrue();

    // Sum total playtime
    @Query("SELECT COALESCE(SUM(g.playtimeHours), 0) FROM Game g")
    Double getTotalPlaytime();

    // Get average rating
    @Query("SELECT COALESCE(AVG(g.rating), 0) FROM Game g WHERE g.rating IS NOT NULL")
    Double getAverageRating();

    // Get games by release year
    List<Game> findByReleaseYear(Integer releaseYear);

    // Get recently added games
    List<Game> findTop10ByOrderByCreatedAtDesc();

    // Get recently completed games
    List<Game> findByStatusOrderByCompletedAtDesc(GameStatus status);

    // Paginated queries
    Page<Game> findByStatus(GameStatus status, Pageable pageable);

    Page<Game> findByPlatform(GamePlatform platform, Pageable pageable);

    // Check if game exists by IGDB ID
    boolean existsByIgdbId(Long igdbId);
}
