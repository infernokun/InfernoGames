import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MaterialModule } from '../../../material.module';
import { Game, GameStatus } from '../../../models/game.model';
import { GameService } from '../../../services/game.service';
import { ApiResponse } from '../../../models/api-response.model';
import { FADE_IN_UP, SLIDE_IN_UP } from '../../../utils/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '../../common/dialog/confirmation-dialog/confirmation-dialog.component';
import { InfernoGamesHelpers } from '../../../utils/helpers';

@Component({
  selector: 'app-game-detail',
  templateUrl: './game-detail.component.html',
  styleUrls: ['./game-detail.component.scss'],
  imports: [
    CommonModule,
    MaterialModule,
    FormsModule,
    RouterModule
  ],
  animations: [FADE_IN_UP, SLIDE_IN_UP]
})
export class GameDetailComponent implements OnInit, OnDestroy {
  helpers: typeof InfernoGamesHelpers = InfernoGamesHelpers;

  game: Game | null = null;
  loading = true;
  gameId: number | null = null;
  syncingSteam = false;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private gameService: GameService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.gameId = +params['id'];
      if (this.gameId) {
        this.loadGame();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadGame(): void {
    if (!this.gameId) return;

    this.loading = true;
    this.gameService.getGameById(this.gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: ApiResponse<Game>) => {
          if (res.data) {
            this.game = res.data;
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading game:', error);
          this.loading = false;
        }
      });
  }

  toggleFavorite(): void {
    if (this.game?.id) {
      this.gameService.toggleFavorite(this.game.id).subscribe({
        next: (res) => {
          if (res.data && this.game) {
            this.game.favorite = res.data.favorite;
          }
        }
      });
    }
  }

  toggleDlc(): void {
    if (this.game?.id) {
      this.gameService.toggleDlc(this.game.id).subscribe({
        next: (res) => {
          if (res.data && this.game) {
            this.game.dlc = res.data.dlc;
            this.snackBar.open(this.game.dlc ? 'Marked as DLC' : 'Unmarked as DLC', 'Close', { duration: 2000 });
          }
        }
      });
    }
  }

  updateStatus(status: GameStatus): void {
    if (this.game?.id) {
      this.gameService.updateGameStatus(this.game.id, status).subscribe({
        next: (res) => {
          if (res.data && this.game) {
            this.game = res.data;
            this.snackBar.open('Status updated!', 'Close', { duration: 2000 });
          }
        },
        error: (error) => {
          console.error('Error updating status:', error);
          this.snackBar.open('Error updating status', 'Close', { duration: 3000 });
        }
      });
    }
  }

  deleteGame(): void {
    if (!this.game?.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Game',
        message: `Are you sure you want to delete "${this.game.title}"?`,
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        confirmButtonColor: 'warn',
        isDestructive: true,
        details: ['This will permanently remove the game from your library.']
      } as ConfirmationDialogData
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && this.game?.id) {
        this.gameService.deleteGame(this.game.id).subscribe({
          next: () => {
            this.snackBar.open('Game deleted successfully', 'Close', { duration: 3000 });
            this.router.navigate(['/games']);
          }
        });
      }
    });
  }

  formatPlaytime(hours: number | undefined): string {
    if (!hours) return '0 hours';
    if (hours < 1) return `${Math.round(hours * 60)} minutes`;
    return `${Math.round(hours)} hours`;
  }

  getAchievementProgress(): number {
    if (!this.game?.totalAchievements) return 0;
    return Math.round((this.game.achievements || 0) / this.game.totalAchievements * 100);
  }

  // Steam-related methods
  hasSteamPlaytimeData(): boolean {
    if (!this.game) return false;
    return (this.game.steamPlaytimeWindowsMinutes || 0) > 0 ||
           (this.game.steamPlaytimeLinuxMinutes || 0) > 0 ||
           (this.game.steamPlaytimeMacMinutes || 0) > 0 ||
           (this.game.steamPlaytimeDeckMinutes || 0) > 0;
  }

  syncSteamData(): void {
    if (!this.game?.id || this.syncingSteam) return;
    
    this.syncingSteam = true;
    this.gameService.syncGameSteamData(this.game.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          if (res.data) {
            this.game = res.data;
            this.snackBar.open('Steam data synced!', 'Close', { duration: 2000 });
          }
          this.syncingSteam = false;
        },
        error: (error) => {
          console.error('Error syncing Steam data:', error);
          this.snackBar.open('Failed to sync Steam data', 'Close', { duration: 3000 });
          this.syncingSteam = false;
        }
      });
  }

  getPlatformIconForSteam(platform: string): string {
    switch (platform) {
      case 'Windows': return 'desktop_windows';
      case 'Linux': return 'terminal';
      case 'Mac': return 'laptop_mac';
      case 'Steam Deck': return 'sports_esports';
      default: return 'devices';
    }
  }

  getPlatformClass(platform: string): string {
    return platform.toLowerCase().replace(' ', '-');
  }

  formatLastSynced(date: Date | undefined): string {
    if (!date) return 'Never';
    const now = new Date();
    const diff = now.getTime() - new Date(date).getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return `${days}d ago`;
  }

  formatDate(date: Date | undefined): string {
    if (!date) return 'Unknown';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  statuses = Object.values(GameStatus);
}