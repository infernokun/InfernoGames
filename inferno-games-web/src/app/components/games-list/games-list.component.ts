import { Component, OnInit, OnDestroy, ViewChild, ElementRef, HostListener } from '@angular/core';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MaterialModule } from '../../material.module';
import { Game, GameStatus, GamePlatform } from '../../models/game.model';
import { GameService } from '../../services/game.service';
import { ApiResponse } from '../../models/api-response.model';
import { FADE_IN_UP, SLIDE_IN_UP, CARD_ANIMATION } from '../../utils/animations';
import { CustomPaginatorComponent } from '../common/custom-paginator/custom-paginator.component';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '../common/dialog/confirmation-dialog/confirmation-dialog.component';

type SortOption = 'title' | 'releaseYear' | 'rating' | 'playtime' | 'status' | 'dateAdded';
type SortDirection = 'asc' | 'desc';
type ViewMode = 'grid' | 'list';

@Component({
  selector: 'app-games-list',
  templateUrl: './games-list.component.html',
  styleUrls: ['./games-list.component.scss'],
  imports: [
    CommonModule,
    MaterialModule,
    FormsModule,
    RouterModule,
    CustomPaginatorComponent
  ],
  animations: [FADE_IN_UP, SLIDE_IN_UP, CARD_ANIMATION]
})
export class GamesListComponent implements OnInit, OnDestroy {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;

  games: Game[] = [];
  filteredGames: Game[] = [];
  displayGames: Game[] = [];
  loading = true;

  // Pagination
  pageSize = 12;
  pageIndex = 0;
  totalGames = 0;
  pageSizeOptions = [6, 12, 24, 48];

  // Filters
  searchTerm = '';
  selectedStatus: GameStatus | '' = '';
  selectedPlatform: GamePlatform | '' = '';
  selectedGenre = '';
  showFavoritesOnly = false;

  // Sort
  currentSortOption: SortOption = 'title';
  currentSortDirection: SortDirection = 'asc';
  viewMode: ViewMode = 'grid';

  // Data for filters
  genres: string[] = [];
  platforms = Object.values(GamePlatform);
  statuses = Object.values(GameStatus);

  sortOptions = [
    { value: 'title', label: 'Title', icon: 'sort_by_alpha' },
    { value: 'releaseYear', label: 'Release Year', icon: 'calendar_today' },
    { value: 'rating', label: 'Rating', icon: 'star' },
    { value: 'playtime', label: 'Playtime', icon: 'schedule' },
    { value: 'status', label: 'Status', icon: 'flag' },
    { value: 'dateAdded', label: 'Date Added', icon: 'history' }
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private gameService: GameService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
    this.loadUserPreferences();
  }

  ngOnInit(): void {
    // Check for query params
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['status']) {
        this.selectedStatus = params['status'] as GameStatus;
      }
      if (params['favorites'] === 'true') {
        this.showFavoritesOnly = true;
      }
      this.loadGames();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadGames(): void {
    this.loading = true;
    this.gameService.getAllGames()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: ApiResponse<Game[]>) => {
          if (res.data) {
            this.games = res.data;
            this.extractGenres();
            this.applyFiltersAndSorting();
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading games:', error);
          this.snackBar.open('Error loading games', 'Close', { duration: 3000 });
          this.loading = false;
        }
      });
  }

  private extractGenres(): void {
    const genreSet = new Set<string>();
    this.games.forEach(g => {
      if (g.genre) genreSet.add(g.genre);
    });
    this.genres = Array.from(genreSet).sort();
  }

  // Pagination
  updatePage(): void {
    const start = this.pageIndex * this.pageSize;
    const end = start + this.pageSize;
    this.displayGames = this.filteredGames.slice(start, end);
  }

  onPageChange(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.pageIndex = event.pageIndex;
    this.updatePage();
    this.saveUserPreferences();
  }

  // Filters
  onSearch(): void {
    this.applyFiltersAndSorting();
  }

  onFilterChange(): void {
    this.applyFiltersAndSorting();
    this.saveUserPreferences();
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.selectedStatus = '';
    this.selectedPlatform = '';
    this.selectedGenre = '';
    this.showFavoritesOnly = false;
    this.applyFiltersAndSorting();
    this.saveUserPreferences();
  }

  hasActiveFilters(): boolean {
    return !!(this.searchTerm || this.selectedStatus || this.selectedPlatform || this.selectedGenre || this.showFavoritesOnly);
  }

  // Sorting
  onSortChange(sortOption: SortOption | string): void {
    const validSortOption = this.isValidSortOption(sortOption) ? sortOption : 'title';

    if (this.currentSortOption === validSortOption) {
      this.currentSortDirection = this.currentSortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.currentSortOption = validSortOption;
      this.currentSortDirection = 'asc';
    }

    this.applyFiltersAndSorting();
    this.saveUserPreferences();
  }

  private isValidSortOption(value: any): value is SortOption {
    return ['title', 'releaseYear', 'rating', 'playtime', 'status', 'dateAdded'].includes(value);
  }

  toggleSortDirection(): void {
    this.currentSortDirection = this.currentSortDirection === 'asc' ? 'desc' : 'asc';
    this.applyFiltersAndSorting();
    this.saveUserPreferences();
  }

  getCurrentSortDetails() {
    return this.sortOptions.find(option => option.value === this.currentSortOption);
  }

  toggleView(): void {
    this.viewMode = this.viewMode === 'grid' ? 'list' : 'grid';
    this.saveUserPreferences();
  }

  private applyFiltersAndSorting(): void {
    let filtered = [...this.games];

    // Search filter
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(g =>
        g.title?.toLowerCase().includes(term) ||
        g.developer?.toLowerCase().includes(term) ||
        g.genre?.toLowerCase().includes(term)
      );
    }

    // Status filter
    if (this.selectedStatus) {
      filtered = filtered.filter(g => g.status === this.selectedStatus);
    }

    // Platform filter
    if (this.selectedPlatform) {
      filtered = filtered.filter(g => g.platform === this.selectedPlatform);
    }

    // Genre filter
    if (this.selectedGenre) {
      filtered = filtered.filter(g => g.genre === this.selectedGenre);
    }

    // Favorites filter
    if (this.showFavoritesOnly) {
      filtered = filtered.filter(g => g.favorite);
    }

    // Sorting
    this.filteredGames = filtered.sort((a, b) => {
      let comparison = 0;

      switch (this.currentSortOption) {
        case 'title':
          comparison = (a.title || '').localeCompare(b.title || '');
          break;
        case 'releaseYear':
          comparison = (a.releaseYear || 0) - (b.releaseYear || 0);
          break;
        case 'rating':
          comparison = (a.rating || 0) - (b.rating || 0);
          break;
        case 'playtime':
          comparison = (a.playtimeHours || 0) - (b.playtimeHours || 0);
          break;
        case 'status':
          const statusOrder = ['IN_PROGRESS', 'NOT_STARTED', 'ON_HOLD', 'COMPLETED', 'DROPPED'];
          comparison = statusOrder.indexOf(a.status || '') - statusOrder.indexOf(b.status || '');
          break;
        case 'dateAdded':
          const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
          const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
          comparison = dateA - dateB;
          break;
        default:
          comparison = (a.title || '').localeCompare(b.title || '');
      }

      return this.currentSortDirection === 'asc' ? comparison : -comparison;
    });

    this.pageIndex = 0;
    this.totalGames = this.filteredGames.length;
    this.updatePage();
  }

  // Actions
  viewGame(id: number | undefined): void {
    if (id) {
      this.router.navigate(['/games', id]);
    }
  }

  editGame(event: Event, id: number | undefined): void {
    event.stopPropagation();
    if (id) {
      this.router.navigate(['/games', id, 'edit']);
    }
  }

  deleteGame(event: Event, game: Game): void {
    event.stopPropagation();
    if (!game.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Game',
        message: `Are you sure you want to delete "${game.title}"?`,
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        confirmButtonColor: 'warn',
        isDestructive: true,
        details: ['This action cannot be undone.']
      } as ConfirmationDialogData
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && game.id) {
        this.gameService.deleteGame(game.id).subscribe({
          next: () => {
            this.snackBar.open('Game deleted successfully', 'Close', { duration: 3000 });
            this.loadGames();
          },
          error: (error) => {
            console.error('Error deleting game:', error);
            this.snackBar.open('Error deleting game', 'Close', { duration: 3000 });
          }
        });
      }
    });
  }

  toggleFavorite(event: Event, game: Game): void {
    event.stopPropagation();
    if (game.id) {
      const wasFavorite = game.favorite;
      // Optimistic update
      game.favorite = !wasFavorite;
      
      this.gameService.toggleFavorite(game.id).subscribe({
        next: (res) => {
          if (res.data) {
            game.favorite = res.data.favorite;
            const message = game.favorite 
              ? `${game.title} added to favorites` 
              : `${game.title} removed from favorites`;
            this.snackBar.open(message, 'Close', { duration: 2000 });
          }
        },
        error: () => {
          // Revert on error
          game.favorite = wasFavorite;
          this.snackBar.open('Error updating favorite', 'Close', { duration: 3000 });
        }
      });
    }
  }

  // Helpers
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

  getStatusTooltip(status: GameStatus | undefined): string {
    switch (status) {
      case GameStatus.COMPLETED: return 'You\'ve finished this game';
      case GameStatus.IN_PROGRESS: return 'Currently playing';
      case GameStatus.ON_HOLD: return 'Taking a break from this one';
      case GameStatus.DROPPED: return 'No longer playing';
      default: return 'Haven\'t started yet';
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

  // Preferences
  private loadUserPreferences(): void {
    const preferences = localStorage.getItem('gamesListPreferences');
    if (preferences) {
      const prefs = JSON.parse(preferences);
      this.viewMode = prefs.viewMode || 'grid';
      this.currentSortOption = prefs.currentSortOption || 'title';
      this.currentSortDirection = prefs.currentSortDirection || 'asc';
      this.pageSize = prefs.pageSize || 12;
    }
  }

  private saveUserPreferences(): void {
    const preferences = {
      viewMode: this.viewMode,
      currentSortOption: this.currentSortOption,
      currentSortDirection: this.currentSortDirection,
      pageSize: this.pageSize
    };
    localStorage.setItem('gamesListPreferences', JSON.stringify(preferences));
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardShortcut(event: KeyboardEvent): void {
    // Ctrl+K or Cmd+K to focus search
    if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
      event.preventDefault();
      this.searchInput?.nativeElement?.focus();
    }
    // Escape to clear search if focused
    if (event.key === 'Escape' && document.activeElement === this.searchInput?.nativeElement) {
      this.searchTerm = '';
      this.onSearch();
      this.searchInput?.nativeElement?.blur();
    }
  }
}
