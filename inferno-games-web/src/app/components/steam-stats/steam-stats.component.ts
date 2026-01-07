import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, forkJoin } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MaterialModule } from '../../material.module';
import { SteamGameInfo, SteamLibraryStats, SteamUserProfile } from '../../models/game.model';
import { GameService } from '../../services/game.service';
import { FADE_IN_UP, CARD_ANIMATION } from '../../utils/animations';

interface TopGame {
  name: string;
  appId: string;
  playtime: number;
  playtimeFormatted: string;
  headerUrl: string;
}

interface PlatformStat {
  name: string;
  icon: string;
  playtime: number;
  playtimeFormatted: string;
  percentage: number;
  color: string;
  games: TopGame[];
}

interface PlaytimeFilter {
  label: string;
  maxHours: number | null; // null means no limit
}

@Component({
  selector: 'app-steam-stats',
  templateUrl: './steam-stats.component.html',
  styleUrls: ['./steam-stats.component.scss'],
  imports: [
    CommonModule,
    MaterialModule,
    RouterModule,
    FormsModule
  ],
  animations: [FADE_IN_UP, CARD_ANIMATION]
})
export class SteamStatsComponent implements OnInit, OnDestroy {
  loading = true;
  steamUser: SteamUserProfile | null = null;
  libraryStats: SteamLibraryStats | null = null;
  steamGames: SteamGameInfo[] = [];
  
  // Playtime filter
  playtimeFilters: PlaytimeFilter[] = [
    { label: 'All Games', maxHours: null },
    { label: 'Under 2000h', maxHours: 2000 },
    { label: 'Under 1000h', maxHours: 1000 },
    { label: 'Under 500h', maxHours: 500 },
    { label: 'Under 200h', maxHours: 200 },
    { label: 'Under 100h', maxHours: 100 }
  ];
  selectedFilter: PlaytimeFilter = this.playtimeFilters[0];
  excludedGames: TopGame[] = []; // Games excluded by current filter
  
  // Computed stats
  topPlayedGames: TopGame[] = [];
  topDeckGames: TopGame[] = [];
  topWindowsGames: TopGame[] = [];
  recentlyPlayed: TopGame[] = [];
  neverPlayed: TopGame[] = [];
  platformStats: PlatformStat[] = [];
  
  // Filtered stats (recalculated based on filter)
  filteredTotalPlaytime: number = 0;
  filteredPlayedGames: number = 0;
  
  // Fun stats
  totalPlaytimeDays: number = 0;
  averagePlaytime: number = 0;
  longestSession: TopGame | null = null;
  
  private destroy$ = new Subject<void>();

  constructor(private gameService: GameService) {}

  ngOnInit(): void {
    this.loadData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadData(): void {
    this.loading = true;
    
    forkJoin({
      steamUser: this.gameService.getSteamUserProfile(),
      libraryStats: this.gameService.getSteamLibraryStats(),
      steamLibrary: this.gameService.getSteamLibrary()
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: ({ steamUser, libraryStats, steamLibrary }) => {
        this.steamUser = steamUser.data || null;
        this.libraryStats = libraryStats.data || null;
        this.steamGames = steamLibrary.data || [];
        
        this.computeStats();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading Steam stats:', error);
        this.loading = false;
      }
    });
  }

  onFilterChange(filter: PlaytimeFilter): void {
    this.selectedFilter = filter;
    this.computeStats();
  }

  private getFilteredGames(): SteamGameInfo[] {
    if (this.selectedFilter.maxHours === null) {
      this.excludedGames = [];
      return this.steamGames;
    }
    
    const maxMinutes = this.selectedFilter.maxHours * 60;
    const excluded = this.steamGames.filter(g => g.playtimeForever >= maxMinutes);
    this.excludedGames = excluded
      .sort((a, b) => b.playtimeForever - a.playtimeForever)
      .map(g => this.createTopGame(g, g.playtimeForever));
    return this.steamGames.filter(g => g.playtimeForever < maxMinutes);
  }

  private computeStats(): void {
    if (!this.steamGames.length || !this.libraryStats) return;
    
    const filteredGames = this.getFilteredGames();
    
    // Calculate filtered totals
    this.filteredTotalPlaytime = filteredGames.reduce((sum, g) => sum + g.playtimeForever, 0);
    this.filteredPlayedGames = filteredGames.filter(g => g.playtimeForever > 0).length;
    
    // Sort games by total playtime
    const sortedByPlaytime = [...filteredGames]
      .filter(g => g.playtimeForever > 0)
      .sort((a, b) => b.playtimeForever - a.playtimeForever);
    
    // Top played games overall
    this.topPlayedGames = sortedByPlaytime.slice(0, 10).map(g => this.createTopGame(g, g.playtimeForever));
    
    // Top Steam Deck games
    const sortedByDeck = [...filteredGames]
      .filter(g => g.playtimeDeckForever > 0)
      .sort((a, b) => b.playtimeDeckForever - a.playtimeDeckForever);
    this.topDeckGames = sortedByDeck.slice(0, 10).map(g => this.createTopGame(g, g.playtimeDeckForever));
    
    // Top Windows games (total minus deck, since Linux = Deck for you)
    const sortedByWindows = [...filteredGames]
      .filter(g => (g.playtimeForever - g.playtimeDeckForever) > 0)
      .sort((a, b) => (b.playtimeForever - b.playtimeDeckForever) - (a.playtimeForever - a.playtimeDeckForever));
    this.topWindowsGames = sortedByWindows.slice(0, 10).map(g => this.createTopGame(g, g.playtimeForever - g.playtimeDeckForever));

    // Recently played
    const sortedByRecent = [...filteredGames]
      .filter(g => g.rtimeLastPlayed > 0)
      .sort((a, b) => b.rtimeLastPlayed - a.rtimeLastPlayed);
    this.recentlyPlayed = sortedByRecent.slice(0, 10).map(g => this.createTopGame(g, g.playtimeForever));
    
    // Never played
    this.neverPlayed = filteredGames
      .filter(g => g.playtimeForever === 0)
      .slice(0, 10)
      .map(g => this.createTopGame(g, 0));
    
    // Platform statistics
    this.computePlatformStats(filteredGames);
    
    // Fun stats
    this.totalPlaytimeDays = Math.floor(this.filteredTotalPlaytime / 60 / 24);
    this.averagePlaytime = sortedByPlaytime.length > 0 
      ? Math.round(this.filteredTotalPlaytime / sortedByPlaytime.length) 
      : 0;
    this.longestSession = this.topPlayedGames[0] || null;
  }

  private createTopGame(game: SteamGameInfo, playtime: number): TopGame {
    return {
      name: game.name,
      appId: game.appId,
      playtime: playtime,
      playtimeFormatted: this.formatPlaytime(playtime),
      headerUrl: this.getGameHeader(game.appId)
    };
  }

  private computePlatformStats(filteredGames: SteamGameInfo[]): void {
    if (!this.libraryStats) return;
    
    // Calculate filtered platform totals
    const filteredDeckPlaytime = filteredGames.reduce((sum, g) => sum + g.playtimeDeckForever, 0);
    const filteredWindowsPlaytime = this.filteredTotalPlaytime - filteredDeckPlaytime;
    
    const totalMinutes = this.filteredTotalPlaytime || 1;
    
    const platforms: PlatformStat[] = [];
    
    // Steam Deck
    if (filteredDeckPlaytime > 0) {
      platforms.push({
        name: 'Steam Deck',
        icon: '🎮',
        playtime: filteredDeckPlaytime,
        playtimeFormatted: this.formatPlaytime(filteredDeckPlaytime),
        percentage: Math.round((filteredDeckPlaytime / totalMinutes) * 100),
        color: '#8b5cf6',
        games: this.topDeckGames.slice(0, 5)
      });
    }
    
    // Windows (everything that's not Deck)
    if (filteredWindowsPlaytime > 0) {
      platforms.push({
        name: 'Windows',
        icon: '🖥️',
        playtime: filteredWindowsPlaytime,
        playtimeFormatted: this.formatPlaytime(filteredWindowsPlaytime),
        percentage: Math.round((filteredWindowsPlaytime / totalMinutes) * 100),
        color: '#3b82f6',
        games: this.topWindowsGames.slice(0, 5)
      });
    }
    
    // Sort by playtime
    this.platformStats = platforms.sort((a, b) => b.playtime - a.playtime);
  }

  formatPlaytime(minutes: number): string {
    if (minutes === 0) return '0h';
    if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours < 100) {
      return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
    }
    return `${hours.toLocaleString()}h`;
  }

  formatLargeNumber(num: number): string {
    if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'k';
    }
    return num.toString();
  }

  getGameHeader(appId: string): string {
    return `https://steamcdn-a.akamaihd.net/steam/apps/${appId}/header.jpg`;
  }

  getGameCapsule(appId: string): string {
    return `https://steamcdn-a.akamaihd.net/steam/apps/${appId}/capsule_184x69.jpg`;
  }

  openInSteam(appId: string): void {
    window.open(`https://store.steampowered.com/app/${appId}`, '_blank');
  }

  getPlayedPercentage(): number {
    if (!this.libraryStats) return 0;
    return this.libraryStats.playedPercentage || 0;
  }

  getUnplayedPercentage(): number {
    return 100 - this.getPlayedPercentage();
  }
  
  getFilteredPlayedPercentage(): number {
    if (!this.libraryStats || !this.libraryStats.totalGames) return 0;
    return Math.round((this.filteredPlayedGames / this.libraryStats.totalGames) * 100);
  }
  
  getTotalExcludedPlaytime(): number {
    return this.excludedGames.reduce((sum, g) => sum + g.playtime, 0);
  }
}