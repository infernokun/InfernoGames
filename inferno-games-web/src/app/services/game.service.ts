import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { Game, GameStats, PlatformStats, GenreStats, GameStatus, GamePlatform, IGDBGame } from '../models/game.model';
import { EnvironmentService } from './environment.service';
import { BaseService } from './base.service';

@Injectable({
  providedIn: 'root',
})
export class GameService extends BaseService {
  private apiUrl: string = '';

  constructor(
    protected override http: HttpClient,
    private environmentService: EnvironmentService
  ) {
    super(http);
    this.apiUrl = `${this.environmentService.settings?.restUrl}/games`;
  }

  // ─── CRUD Operations ────────────────────────────────────────────────────────

  getAllGames(): Observable<ApiResponse<Game[]>> {
    return this.get<ApiResponse<Game[]>>(this.apiUrl).pipe(
      map(response => ({
        ...response,
        data: response.data?.map(g => new Game(g)) || []
      })),
      catchError(error => {
        console.error('Error fetching games:', error);
        return of({ code: 500, data: [], message: 'Failed to fetch games', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  getGameById(id: number): Observable<ApiResponse<Game>> {
    return this.get<ApiResponse<Game>>(`${this.apiUrl}/${id}`).pipe(
      map(response => ({
        ...response,
        data: response.data ? new Game(response.data) : undefined
      })),
      catchError(error => {
        console.error('Error fetching game:', error);
        return of({ code: 500, data: undefined, message: 'Failed to fetch game', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  createGame(game: Partial<Game>): Observable<ApiResponse<Game>> {
    return this.post<ApiResponse<Game>>(this.apiUrl, game).pipe(
      map(response => ({
        ...response,
        data: response.data ? new Game(response.data) : undefined
      })),
      catchError(error => {
        console.error('Error creating game:', error);
        return of({ code: 500, data: undefined, message: 'Failed to create game', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  updateGame(id: number, game: Partial<Game>): Observable<ApiResponse<Game>> {
    return this.put<ApiResponse<Game>>(`${this.apiUrl}/${id}`, game).pipe(
      map(response => ({
        ...response,
        data: response.data ? new Game(response.data) : undefined
      })),
      catchError(error => {
        console.error('Error updating game:', error);
        return of({ code: 500, data: undefined, message: 'Failed to update game', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  deleteGame(id: number): Observable<ApiResponse<void>> {
    return this.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`).pipe(
      catchError(error => {
        console.error('Error deleting game:', error);
        return of({ code: 500, data: undefined, message: 'Failed to delete game', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  // ─── Status & Favorite Operations ───────────────────────────────────────────

  updateGameStatus(id: number, status: GameStatus): Observable<ApiResponse<Game>> {
    return this.post<ApiResponse<Game>>(`${this.apiUrl}/${id}/status?status=${status}`, {}).pipe(
      map(response => ({
        ...response,
        data: response.data ? new Game(response.data) : undefined
      })),
      catchError(error => {
        console.error('Error updating game status:', error);
        return of({ code: 500, data: undefined, message: 'Failed to update status', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  toggleFavorite(id: number): Observable<ApiResponse<Game>> {
    return this.post<ApiResponse<Game>>(`${this.apiUrl}/${id}/favorite`, {}).pipe(
      map(response => ({
        ...response,
        data: response.data ? new Game(response.data) : undefined
      })),
      catchError(error => {
        console.error('Error toggling favorite:', error);
        return of({ code: 500, data: undefined, message: 'Failed to toggle favorite', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  // ─── Query Operations ───────────────────────────────────────────────────────

  searchGames(query: string): Observable<ApiResponse<Game[]>> {
    return this.get<ApiResponse<Game[]>>(`${this.apiUrl}/search?query=${encodeURIComponent(query)}`).pipe(
      map(response => ({
        ...response,
        data: response.data?.map(g => new Game(g)) || []
      })),
      catchError(error => {
        console.error('Error searching games:', error);
        return of({ code: 500, data: [], message: 'Failed to search games', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  advancedSearch(
    title?: string,
    status?: GameStatus,
    platform?: GamePlatform,
    genre?: string
  ): Observable<ApiResponse<Game[]>> {
    let params = new HttpParams();
    if (title) params = params.set('title', title);
    if (status) params = params.set('status', status);
    if (platform) params = params.set('platform', platform);
    if (genre) params = params.set('genre', genre);

    return this.http.get<ApiResponse<Game[]>>(`${this.apiUrl}/search/advanced`, { params }).pipe(
      map(response => ({
        ...response,
        data: response.data?.map(g => new Game(g)) || []
      })),
      catchError(error => {
        console.error('Error in advanced search:', error);
        return of({ code: 500, data: [], message: 'Failed to search games', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  getGamesByStatus(status: GameStatus): Observable<ApiResponse<Game[]>> {
    return this.get<ApiResponse<Game[]>>(`${this.apiUrl}/status/${status}`).pipe(
      map(response => ({
        ...response,
        data: response.data?.map(g => new Game(g)) || []
      })),
      catchError(error => {
        console.error('Error fetching games by status:', error);
        return of({ code: 500, data: [], message: 'Failed to fetch games', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  getGamesByPlatform(platform: GamePlatform): Observable<ApiResponse<Game[]>> {
    return this.get<ApiResponse<Game[]>>(`${this.apiUrl}/platform/${platform}`).pipe(
      map(response => ({
        ...response,
        data: response.data?.map(g => new Game(g)) || []
      })),
      catchError(error => {
        console.error('Error fetching games by platform:', error);
        return of({ code: 500, data: [], message: 'Failed to fetch games', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  getFavoriteGames(): Observable<ApiResponse<Game[]>> {
    return this.get<ApiResponse<Game[]>>(`${this.apiUrl}/favorites`).pipe(
      map(response => ({
        ...response,
        data: response.data?.map(g => new Game(g)) || []
      })),
      catchError(error => {
        console.error('Error fetching favorite games:', error);
        return of({ code: 500, data: [], message: 'Failed to fetch favorites', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  getRecentlyAddedGames(): Observable<ApiResponse<Game[]>> {
    return this.get<ApiResponse<Game[]>>(`${this.apiUrl}/recent`).pipe(
      map(response => ({
        ...response,
        data: response.data?.map(g => new Game(g)) || []
      })),
      catchError(error => {
        console.error('Error fetching recent games:', error);
        return of({ code: 500, data: [], message: 'Failed to fetch recent games', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  getRecentlyCompletedGames(): Observable<ApiResponse<Game[]>> {
    return this.get<ApiResponse<Game[]>>(`${this.apiUrl}/completed`).pipe(
      map(response => ({
        ...response,
        data: response.data?.map(g => new Game(g)) || []
      })),
      catchError(error => {
        console.error('Error fetching completed games:', error);
        return of({ code: 500, data: [], message: 'Failed to fetch completed games', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  // ─── Statistics ─────────────────────────────────────────────────────────────

  getGameStats(): Observable<ApiResponse<GameStats>> {
    return this.get<ApiResponse<GameStats>>(`${this.apiUrl}/stats`).pipe(
      catchError(error => {
        console.error('Error fetching game stats:', error);
        return of({
          code: 500,
          data: {
            totalGames: 0,
            completedGames: 0,
            inProgressGames: 0,
            notStartedGames: 0,
            totalPlaytime: 0,
            completionRate: 0
          },
          message: 'Failed to fetch stats',
          type: 'ERROR' as any,
          timeMs: 0
        });
      })
    );
  }

  getPlatformStats(): Observable<ApiResponse<PlatformStats[]>> {
    // Platform stats are included in the main stats endpoint
    return this.get<ApiResponse<GameStats>>(`${this.apiUrl}/stats`).pipe(
      map(response => {
        const platformBreakdown = response.data?.platformBreakdown || {};
        const stats: PlatformStats[] = Object.entries(platformBreakdown).map(([platform, count]) => ({
          platform: platform as GamePlatform,
          gameCount: count as number,
          completedCount: 0, // Not available from this endpoint
          percentage: 0 // Will be calculated in component
        }));
        return {
          ...response,
          data: stats
        };
      }),
      catchError(error => {
        console.error('Error fetching platform stats:', error);
        return of({ code: 500, data: [], message: 'Failed to fetch platform stats', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  getGenreStats(): Observable<ApiResponse<GenreStats[]>> {
    // Genre stats are included in the main stats endpoint
    return this.get<ApiResponse<GameStats>>(`${this.apiUrl}/stats`).pipe(
      map(response => {
        const genreBreakdown = response.data?.genreBreakdown || {};
        const stats: GenreStats[] = Object.entries(genreBreakdown).map(([genre, count]) => ({
          genre,
          gameCount: count as number,
          percentage: 0 // Will be calculated in component
        }));
        return {
          ...response,
          data: stats.sort((a, b) => b.gameCount - a.gameCount)
        };
      }),
      catchError(error => {
        console.error('Error fetching genre stats:', error);
        return of({ code: 500, data: [], message: 'Failed to fetch genre stats', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  // ─── IGDB Integration ───────────────────────────────────────────────────────

  searchIGDB(query: string): Observable<ApiResponse<IGDBGame[]>> {
    return this.get<ApiResponse<IGDBGame[]>>(`${this.apiUrl}/igdb/search?query=${encodeURIComponent(query)}`).pipe(
      catchError(error => {
        console.error('Error searching IGDB:', error);
        return of({ code: 500, data: [], message: 'Failed to search IGDB', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  getIGDBGameById(igdbId: number): Observable<ApiResponse<IGDBGame>> {
    return this.get<ApiResponse<IGDBGame>>(`${this.apiUrl}/igdb/${igdbId}`).pipe(
      catchError(error => {
        console.error('Error fetching IGDB game:', error);
        return of({ code: 500, data: undefined, message: 'Failed to fetch IGDB game', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  getPopularIGDBGames(limit: number = 20): Observable<ApiResponse<IGDBGame[]>> {
    return this.get<ApiResponse<IGDBGame[]>>(`${this.apiUrl}/igdb/popular?limit=${limit}`).pipe(
      catchError(error => {
        console.error('Error fetching popular IGDB games:', error);
        return of({ code: 500, data: [], message: 'Failed to fetch popular games', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  getRecentIGDBReleases(limit: number = 20): Observable<ApiResponse<IGDBGame[]>> {
    return this.get<ApiResponse<IGDBGame[]>>(`${this.apiUrl}/igdb/recent?limit=${limit}`).pipe(
      catchError(error => {
        console.error('Error fetching recent IGDB releases:', error);
        return of({ code: 500, data: [], message: 'Failed to fetch recent releases', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  getUpcomingIGDBGames(limit: number = 20): Observable<ApiResponse<IGDBGame[]>> {
    return this.get<ApiResponse<IGDBGame[]>>(`${this.apiUrl}/igdb/upcoming?limit=${limit}`).pipe(
      catchError(error => {
        console.error('Error fetching upcoming IGDB games:', error);
        return of({ code: 500, data: [], message: 'Failed to fetch upcoming games', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  importFromIGDB(igdbId: number): Observable<ApiResponse<Game>> {
    return this.post<ApiResponse<Game>>(`${this.apiUrl}/igdb/import/${igdbId}`, {}).pipe(
      map(response => ({
        ...response,
        data: response.data ? new Game(response.data) : undefined
      })),
      catchError(error => {
        console.error('Error importing from IGDB:', error);
        return of({ code: 500, data: undefined, message: 'Failed to import from IGDB', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  refreshFromIGDB(gameId: number): Observable<ApiResponse<Game>> {
    return this.post<ApiResponse<Game>>(`${this.apiUrl}/${gameId}/igdb/refresh`, {}).pipe(
      map(response => ({
        ...response,
        data: response.data ? new Game(response.data) : undefined
      })),
      catchError(error => {
        console.error('Error refreshing from IGDB:', error);
        return of({ code: 500, data: undefined, message: 'Failed to refresh from IGDB', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }

  // ─── Cache Management ───────────────────────────────────────────────────────

  clearCaches(): Observable<ApiResponse<void>> {
    return this.delete<ApiResponse<void>>(`${this.apiUrl}/cache`).pipe(
      catchError(error => {
        console.error('Error clearing caches:', error);
        return of({ code: 500, data: undefined, message: 'Failed to clear caches', type: 'ERROR' as any, timeMs: 0 });
      })
    );
  }
}
