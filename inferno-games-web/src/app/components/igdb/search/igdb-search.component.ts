import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MaterialModule } from '../../../material.module';
import { IGDBGame } from '../../../models/game.model';
import { GameService } from '../../../services/game.service';
import { FADE_IN_UP, CARD_ANIMATION } from '../../../utils/animations';

@Component({
  selector: 'app-igdb-search',
  templateUrl: './igdb-search.component.html',
  styleUrls: ['./igdb-search.component.scss'],
  imports: [
    CommonModule,
    MaterialModule,
    FormsModule,
    RouterModule
  ],
  animations: [FADE_IN_UP, CARD_ANIMATION]
})
export class IgdbSearchComponent implements OnInit, OnDestroy {
  searchQuery = '';
  searchResults: IGDBGame[] = [];
  popularGames: IGDBGame[] = [];
  recentReleases: IGDBGame[] = [];
  upcomingGames: IGDBGame[] = [];
  
  loading = false;
  searching = false;
  importingId: number | null = null;
  addedGameIds: Set<number> = new Set(); // Track games that have been added in this session
  backlogGameIgdbIds: Set<number> = new Set(); // Track games already in the backlog
  showSteamOnly = false; // Filter toggle for Steam library games
  
  activeTab: 'search' | 'popular' | 'recent' | 'upcoming' = 'search';
  
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private gameService: GameService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    // Load existing backlog games to check for duplicates
    this.loadBacklogGames();
    
    // Setup debounced search
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(query => {
      if (query.trim().length >= 2) {
        this.performSearch(query);
      } else {
        this.searchResults = [];
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadBacklogGames(): void {
    this.gameService.getAllGames()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          if (res.data) {
            res.data.forEach(game => {
              if (game.igdbId) {
                this.backlogGameIgdbIds.add(game.igdbId);
              }
            });
          }
        },
        error: (error) => {
          console.error('Error loading backlog games:', error);
        }
      });
  }

  onSearchInput(): void {
    this.searchSubject.next(this.searchQuery);
  }

  performSearch(query: string): void {
    this.searching = true;
    this.gameService.searchIGDB(query)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.searchResults = res.data || [];
          this.searching = false;
        },
        error: (error) => {
          console.error('Error searching IGDB:', error);
          this.snackBar.open('Error searching games', 'Close', { duration: 3000 });
          this.searching = false;
        }
      });
  }

  loadPopularGames(): void {
    this.loading = true;
    this.gameService.getPopularIGDBGames(20)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.popularGames = res.data || [];
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading popular games:', error);
          this.loading = false;
        }
      });
  }

  loadRecentReleases(): void {
    if (this.recentReleases.length > 0) return;
    
    this.loading = true;
    this.gameService.getRecentIGDBReleases(20)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.recentReleases = res.data || [];
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading recent releases:', error);
          this.loading = false;
        }
      });
  }

  loadUpcomingGames(): void {
    if (this.upcomingGames.length > 0) return;
    
    this.loading = true;
    this.gameService.getUpcomingIGDBGames(20)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.upcomingGames = res.data || [];
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading upcoming games:', error);
          this.loading = false;
        }
      });
  }

  onTabChange(tab: 'search' | 'popular' | 'recent' | 'upcoming'): void {
    this.activeTab = tab;
    
    switch (tab) {
      case 'popular':
        if (this.popularGames.length === 0) this.loadPopularGames();
        break;
      case 'recent':
        this.loadRecentReleases();
        break;
      case 'upcoming':
        this.loadUpcomingGames();
        break;
    }
  }

  toggleSteamFilter(): void {
    this.showSteamOnly = !this.showSteamOnly;
  }

  importGame(game: IGDBGame): void {
    if (!game.igdbId) return;
    
    this.importingId = game.igdbId;
    this.gameService.importFromIGDB(game.igdbId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.importingId = null;
          if (res.data?.id) {
            this.addedGameIds.add(game.igdbId!); // Mark as added
            this.backlogGameIgdbIds.add(game.igdbId!); // Also add to backlog set
            this.snackBar.open(`${game.name} added to your library!`, 'View', { duration: 5000 })
              .onAction().subscribe(() => {
                this.router.navigate(['/games', res.data!.id]);
              });
          } else {
            this.addedGameIds.add(game.igdbId!); // Mark as added even without ID
            this.backlogGameIgdbIds.add(game.igdbId!);
            this.snackBar.open('Game added to your library!', 'Close', { duration: 3000 });
          }
        },
        error: (error) => {
          console.error('Error importing game:', error);
          this.snackBar.open('Error adding game', 'Close', { duration: 3000 });
          this.importingId = null;
        }
      });
  }

  isGameAdded(game: IGDBGame): boolean {
    return game.igdbId ? this.addedGameIds.has(game.igdbId) : false;
  }

  isGameInBacklog(game: IGDBGame): boolean {
    // Check if game is in backlog but wasn't just added in this session
    if (!game.igdbId) return false;
    return this.backlogGameIgdbIds.has(game.igdbId) && !this.addedGameIds.has(game.igdbId);
  }

  getDisplayGames(): IGDBGame[] {
    let games: IGDBGame[];
    
    switch (this.activeTab) {
      case 'search':
        games = this.searchResults;
        break;
      case 'popular':
        games = this.popularGames;
        break;
      case 'recent':
        games = this.recentReleases;
        break;
      case 'upcoming':
        games = this.upcomingGames;
        break;
      default:
        games = [];
    }
    
    // Apply Steam filter if enabled
    if (this.showSteamOnly) {
      games = games.filter(g => g.steamAppId);
    }
    
    return games;
  }

  getFilteredCount(games: IGDBGame[]): number {
    if (this.showSteamOnly) {
      return games.filter(g => g.steamAppId).length;
    }
    return games.length;
  }

  formatRating(rating: number | undefined): string {
    if (!rating) return 'N/A';
    return (rating / 10).toFixed(1);
  }

  formatDate(date: Date | string | undefined): string {
    if (!date) return 'TBA';
    const d = new Date(date);
    return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  trackByGame(index: number, game: IGDBGame): number {
    return game.igdbId || index;
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchResults = [];
  }
}