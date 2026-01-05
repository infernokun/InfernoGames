import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MaterialModule } from '../../material.module';
import { Game, GameStatus, GameStats, PlatformStats, GenreStats } from '../../models/game.model';
import { GameService } from '../../services/game.service';
import { ApiResponse } from '../../models/api-response.model';
import { FADE_IN_UP, SLIDE_IN_UP, CARD_ANIMATION } from '../../utils/animations';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [
    CommonModule,
    MaterialModule,
    FormsModule,
    RouterModule,
  ],
  animations: [FADE_IN_UP, SLIDE_IN_UP, CARD_ANIMATION]
})
export class DashboardComponent implements OnInit, OnDestroy {
  games: Game[] = [];
  stats: GameStats | null = null;
  platformStats: PlatformStats[] = [];
  genreStats: GenreStats[] = [];
  loading = true;

  // Quick filters
  inProgressGames: Game[] = [];
  recentlyCompletedGames: Game[] = [];
  favoriteGames: Game[] = [];
  backlogGames: Game[] = [];

  private destroy$ = new Subject<void>();

  constructor(
    private gameService: GameService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadDashboardData(): void {
    this.loading = true;

    this.gameService.getAllGames()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: ApiResponse<Game[]>) => {
          if (res.data) {
            this.games = res.data;
            this.processGames();
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading games:', error);
          this.loading = false;
        }
      });

    this.gameService.getGameStats()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: ApiResponse<GameStats>) => {
          if (res.data) {
            this.stats = res.data;
          }
        }
      });

    this.gameService.getPlatformStats()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: ApiResponse<PlatformStats[]>) => {
          if (res.data) {
            this.platformStats = res.data;
          }
        }
      });

    this.gameService.getGenreStats()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: ApiResponse<GenreStats[]>) => {
          if (res.data) {
            this.genreStats = res.data;
          }
        }
      });
  }

  private processGames(): void {
    this.inProgressGames = this.games
      .filter(g => g.status === GameStatus.IN_PROGRESS)
      .slice(0, 4);

    this.recentlyCompletedGames = this.games
      .filter(g => g.status === GameStatus.COMPLETED)
      .sort((a, b) => {
        const dateA = a.completedAt ? new Date(a.completedAt).getTime() : 0;
        const dateB = b.completedAt ? new Date(b.completedAt).getTime() : 0;
        return dateB - dateA;
      })
      .slice(0, 4);

    this.favoriteGames = this.games
      .filter(g => g.favorite)
      .slice(0, 4);

    this.backlogGames = this.games
      .filter(g => g.status === GameStatus.NOT_STARTED)
      .slice(0, 6);
  }

  getStatusColor(status: GameStatus | undefined): string {
    switch (status) {
      case GameStatus.COMPLETED: return 'completed';
      case GameStatus.IN_PROGRESS: return 'in-progress';
      case GameStatus.ON_HOLD: return 'on-hold';
      case GameStatus.DROPPED: return 'dropped';
      default: return 'not-started';
    }
  }

  getStatusLabel(status: GameStatus | undefined): string {
    switch (status) {
      case GameStatus.COMPLETED: return 'Completed';
      case GameStatus.IN_PROGRESS: return 'Playing';
      case GameStatus.ON_HOLD: return 'On Hold';
      case GameStatus.DROPPED: return 'Dropped';
      default: return 'Backlog';
    }
  }

  getPlatformIcon(platform: string): string {
    switch (platform) {
      case 'PC': return 'computer';
      case 'PLAYSTATION_5':
      case 'PLAYSTATION_4': return 'sports_esports';
      case 'XBOX_SERIES':
      case 'XBOX_ONE': return 'gamepad';
      case 'NINTENDO_SWITCH': return 'videogame_asset';
      case 'STEAM_DECK': return 'tablet_android';
      case 'MOBILE': return 'phone_android';
      default: return 'devices';
    }
  }

  getPlatformLabel(platform: string): string {
    switch (platform) {
      case 'PC': return 'PC';
      case 'PLAYSTATION_5': return 'PS5';
      case 'PLAYSTATION_4': return 'PS4';
      case 'XBOX_SERIES': return 'Xbox Series';
      case 'XBOX_ONE': return 'Xbox One';
      case 'NINTENDO_SWITCH': return 'Switch';
      case 'STEAM_DECK': return 'Steam Deck';
      case 'MOBILE': return 'Mobile';
      default: return 'Other';
    }
  }

  formatPlaytime(hours: number | undefined): string {
    if (!hours) return '0h';
    if (hours < 1) return `${Math.round(hours * 60)}m`;
    return `${Math.round(hours)}h`;
  }

  trackByGame(index: number, game: Game): number {
    return game.id || index;
  }

  startPlaying(event: Event, game: Game): void {
    event.stopPropagation();
    event.preventDefault();
    
    if (!game.id) return;
    
    this.gameService.updateGameStatus(game.id, GameStatus.IN_PROGRESS)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          if (res.data) {
            // Move game from backlog to in progress
            this.backlogGames = this.backlogGames.filter(g => g.id !== game.id);
            game.status = GameStatus.IN_PROGRESS;
            this.inProgressGames = [game, ...this.inProgressGames].slice(0, 4);
            this.snackBar.open(`Started playing ${game.title}!`, 'View', { duration: 3000 })
              .onAction().subscribe(() => {
                // Navigate to game detail - we'll need Router for this
              });
          }
        },
        error: (error) => {
          console.error('Error updating game status:', error);
          this.snackBar.open('Error updating game status', 'Close', { duration: 3000 });
        }
      });
  }
}
