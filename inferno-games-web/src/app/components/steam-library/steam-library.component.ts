import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { MaterialModule } from '../../material.module';
import { SteamGameInfo, Game } from '../../models/game.model';
import { GameService } from '../../services/game.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FADE_IN_UP, CARD_ANIMATION } from '../../utils/animations';

interface GenreInfo {
  name: string;
  count: number;
  percentage: number;
  playtime: number;
  playtimeFormatted: string;
}

@Component({
  selector: 'app-steam-library',
  templateUrl: './steam-library.component.html',
  styleUrls: ['./steam-library.component.scss'],
  imports: [
    CommonModule,
    MaterialModule,
    FormsModule,
    RouterModule
  ],
  animations: [FADE_IN_UP, CARD_ANIMATION]
})
export class SteamLibraryComponent implements OnInit, OnDestroy {
  steamGames: SteamGameInfo[] = [];
  filteredGames: SteamGameInfo[] = [];
  displayedGames: SteamGameInfo[] = [];

  loading = true;
  enrichingIgdb = false;
  importingAppId: string | null = null;

  // Search & Filter
  searchQuery = '';
  sortBy: 'name' | 'playtime' | 'recent' = 'playtime';
  sortOrder: 'asc' | 'desc' = 'desc';
  filterPlayed: 'all' | 'played' | 'unplayed' = 'all';
  filterInBacklog: 'all' | 'inBacklog' | 'notInBacklog' = 'all';

  // Genre filtering
  genres: GenreInfo[] = [];
  selectedGenres: Set<string> = new Set();
  showGenrePanel = false;
  genreSortBy: 'count' | 'playtime' | 'name' = 'count';

  // Pagination
  pageSize = 24;
  currentPage = 0;
  totalPages = 0;

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private gameService: GameService,
    private router: Router,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit(): void {
    this.loadData();

    // Setup debounced search
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.applyFiltersAndSort();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadData(): void {
    this.loading = true;

    this.gameService.getSteamLibraryWithGenres()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.steamGames = response.data || [];

          // Compute genre statistics
          this.computeGenreStats();

          this.loading = false;
          this.applyFiltersAndSort();
        },
        error: (error) => {
          console.error('Error loading data:', error);
          this.loading = false;
          this.snackBar.open('Error loading Steam library', 'Close', { duration: 3000 });
        }
      });
  }

  onSearchInput(): void {
    this.searchSubject.next(this.searchQuery);
  }

  applyFiltersAndSort(): void {
    let games = [...this.steamGames];

    // Apply search filter
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      games = games.filter(g => g.name.toLowerCase().includes(query));
    }

    // Apply played filter
    if (this.filterPlayed === 'played') {
      games = games.filter(g => g.playtimeForever > 0);
    } else if (this.filterPlayed === 'unplayed') {
      games = games.filter(g => g.playtimeForever === 0);
    }

    // Apply backlog filter
    if (this.filterInBacklog === 'inBacklog') {
      games = games.filter(g => g.inBacklog);
    } else if (this.filterInBacklog === 'notInBacklog') {
      games = games.filter(g => !g.inBacklog);
    }

    // Apply genre filter
    if (this.selectedGenres.size > 0) {
      games = games.filter(g => {
        if (!g.genres || g.genres.length === 0) return false;
        return g.genres.some(genre => this.selectedGenres.has(genre));
      });
    }

    // Apply sorting
    games.sort((a, b) => {
      let comparison = 0;

      switch (this.sortBy) {
        case 'name':
          comparison = a.name.localeCompare(b.name);
          break;
        case 'playtime':
          comparison = a.playtimeForever - b.playtimeForever;
          break;
        case 'recent':
          comparison = a.rtimeLastPlayed - b.rtimeLastPlayed;
          break;
      }

      return this.sortOrder === 'desc' ? -comparison : comparison;
    });

    this.filteredGames = games;
    this.totalPages = Math.ceil(this.filteredGames.length / this.pageSize);
    this.currentPage = 0;
    this.updateDisplayedGames();
  }

  updateDisplayedGames(): void {
    const start = this.currentPage * this.pageSize;
    const end = start + this.pageSize;
    this.displayedGames = this.filteredGames.slice(start, end);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.updateDisplayedGames();
    // Scroll to top of results
    document.querySelector('.steam-library-container')?.scrollIntoView({ behavior: 'smooth' });
  }

  onSortChange(sortBy: 'name' | 'playtime' | 'recent'): void {
    if (this.sortBy === sortBy) {
      this.sortOrder = this.sortOrder === 'desc' ? 'asc' : 'desc';
    } else {
      this.sortBy = sortBy;
      this.sortOrder = sortBy === 'name' ? 'asc' : 'desc';
    }
    this.applyFiltersAndSort();
  }

  onFilterPlayedChange(filter: 'all' | 'played' | 'unplayed'): void {
    this.filterPlayed = filter;
    this.applyFiltersAndSort();
  }

  onFilterBacklogChange(filter: 'all' | 'inBacklog' | 'notInBacklog'): void {
    this.filterInBacklog = filter;
    this.applyFiltersAndSort();
  }

  importToBacklog(game: SteamGameInfo): void {
    this.importingAppId = game.appId;

    // First, search IGDB for the game
    this.gameService.searchIGDB(game.name)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          const igdbGames = res.data || [];
          // Try to find an exact or close match
          const match = igdbGames.find(g =>
            g.name.toLowerCase() === game.name.toLowerCase() ||
            g.steamAppId === game.appId
          ) || igdbGames[0];

          if (match?.igdbId) {
            // Import from IGDB
            this.gameService.importFromIGDB(match.igdbId)
              .pipe(takeUntil(this.destroy$))
              .subscribe({
                next: (importRes) => {
                  this.importingAppId = null;
                  if (importRes.data?.id) {
                    // Update local state
                    game.inBacklog = true;
                    game.backlogGameId = importRes.data.id;
                    this.snackBar.open(`${game.name} added to backlog!`, 'View', { duration: 5000 })
                      .onAction().subscribe(() => {
                        this.router.navigate(['/games', importRes.data!.id]);
                      });
                  }
                },
                error: (error) => {
                  console.error('Error importing game:', error);
                  this.importingAppId = null;
                  this.snackBar.open('Error adding game to backlog', 'Close', { duration: 3000 });
                }
              });
          } else {
            // No IGDB match found, create basic entry
            this.createBasicGame(game);
          }
        },
        error: () => {
          // IGDB search failed, create basic entry
          this.createBasicGame(game);
        }
      });
  }

  private createBasicGame(game: SteamGameInfo): void {
    const newGame: Partial<Game> = {
      title: game.name,
      steamAppId: game.appId,
      coverImageUrl: game.imgIconUrl ? `https://media.steampowered.com/steamcommunity/public/images/apps/${game.appId}/${game.imgIconUrl}.jpg` : undefined
    };

    this.gameService.createGame(newGame)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.importingAppId = null;
          if (res.data?.id) {
            game.inBacklog = true;
            game.backlogGameId = res.data.id;
            this.snackBar.open(`${game.name} added to backlog!`, 'View', { duration: 5000 })
              .onAction().subscribe(() => {
                this.router.navigate(['/games', res.data!.id]);
              });
          }
        },
        error: (error) => {
          console.error('Error creating game:', error);
          this.importingAppId = null;
          this.snackBar.open('Error adding game to backlog', 'Close', { duration: 3000 });
        }
      });
  }

  viewInBacklog(game: SteamGameInfo): void {
    if (game.backlogGameId) {
      this.router.navigate(['/games', game.backlogGameId]);
    }
  }

  openInSteam(appId: string): void {
    window.open(`https://store.steampowered.com/app/${appId}`, '_blank');
  }

  formatPlaytime(minutes: number): string {
    if (minutes === 0) return 'Never played';
    if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours < 100) {
      return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
    }
    return `${hours}h`;
  }

  formatLastPlayed(timestamp: number): string {
    if (!timestamp || timestamp === 0) return 'Never';
    const date = new Date(timestamp * 1000);
    const now = new Date();
    const diffDays = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return `${diffDays} days ago`;
    if (diffDays < 30) return `${Math.floor(diffDays / 7)} weeks ago`;
    if (diffDays < 365) return `${Math.floor(diffDays / 30)} months ago`;
    return `${Math.floor(diffDays / 365)} years ago`;
  }

  getGameIcon(game: SteamGameInfo): string {
    if (game.imgIconUrl) {
      return `https://media.steampowered.com/steamcommunity/public/images/apps/${game.appId}/${game.imgIconUrl}.jpg`;
    }
    return '';
  }

  getGameHeader(appId: string): string {
    return `https://steamcdn-a.akamaihd.net/steam/apps/${appId}/header.jpg`;
  }

  trackByAppId(index: number, game: SteamGameInfo): string {
    return game.appId;
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.applyFiltersAndSort();
  }

  get playedCount(): number {
    return this.steamGames.filter(g => g.playtimeForever > 0).length;
  }

  get unplayedCount(): number {
    return this.steamGames.filter(g => g.playtimeForever === 0).length;
  }

  get inBacklogCount(): number {
    return this.steamGames.filter(g => g.inBacklog).length;
  }

  // Genre-related methods
  computeGenreStats(): void {
    const genreMap = new Map<string, { count: number; playtime: number }>();

    // Only count games that are in backlog (have genre data)
    const gamesWithGenres = this.steamGames.filter(g => g.genres && g.genres.length > 0);

    gamesWithGenres.forEach(game => {
      game.genres!.forEach(genre => {
        const existing = genreMap.get(genre) || { count: 0, playtime: 0 };
        existing.count++;
        existing.playtime += game.playtimeForever;
        genreMap.set(genre, existing);
      });
    });

    const totalGamesWithGenres = gamesWithGenres.length;

    this.genres = Array.from(genreMap.entries())
      .map(([name, data]) => ({
        name,
        count: data.count,
        percentage: totalGamesWithGenres > 0 ? Math.round((data.count / totalGamesWithGenres) * 100) : 0,
        playtime: data.playtime,
        playtimeFormatted: this.formatPlaytime(data.playtime)
      }));

    this.sortGenres();
  }

  sortGenres(): void {
    this.genres.sort((a, b) => {
      switch (this.genreSortBy) {
        case 'count':
          return b.count - a.count;
        case 'playtime':
          return b.playtime - a.playtime;
        case 'name':
          return a.name.localeCompare(b.name);
        default:
          return 0;
      }
    });
  }

  onGenreSortChange(sortBy: 'count' | 'playtime' | 'name'): void {
    this.genreSortBy = sortBy;
    this.sortGenres();
  }

  toggleGenrePanel(): void {
    this.showGenrePanel = !this.showGenrePanel;
  }

  toggleGenre(genre: string): void {
    if (this.selectedGenres.has(genre)) {
      this.selectedGenres.delete(genre);
    } else {
      this.selectedGenres.add(genre);
    }
    this.applyFiltersAndSort();
  }

  isGenreSelected(genre: string): boolean {
    return this.selectedGenres.has(genre);
  }

  clearGenreFilters(): void {
    this.selectedGenres.clear();
    this.applyFiltersAndSort();
  }

  get gamesWithGenreDataCount(): number {
    return this.steamGames.filter(g => g.genres && g.genres.length > 0).length;
  }

  get maxGenreCount(): number {
    if (this.genres.length === 0) return 1;
    return Math.max(...this.genres.map(g => g.count));
  }

  get maxGenrePlaytime(): number {
    if (this.genres.length === 0) return 1;
    return Math.max(...this.genres.map(g => g.playtime));
  }

  getGenreBarWidth(genre: GenreInfo): number {
    if (this.genreSortBy === 'playtime') {
      return this.maxGenrePlaytime > 0 ? (genre.playtime / this.maxGenrePlaytime) * 100 : 0;
    }
    return this.maxGenreCount > 0 ? (genre.count / this.maxGenreCount) * 100 : 0;
  }

  getGenreColor(index: number): string {
    const colors = [
      '#6366f1', // Indigo
      '#8b5cf6', // Violet
      '#a855f7', // Purple
      '#d946ef', // Fuchsia
      '#ec4899', // Pink
      '#f43f5e', // Rose
      '#ef4444', // Red
      '#f97316', // Orange
      '#f59e0b', // Amber
      '#eab308', // Yellow
      '#84cc16', // Lime
      '#22c55e', // Green
      '#10b981', // Emerald
      '#14b8a6', // Teal
      '#06b6d4', // Cyan
      '#0ea5e9', // Sky
      '#3b82f6', // Blue
    ];
    return colors[index % colors.length];
  }
}
