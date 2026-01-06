import { Component, OnDestroy, OnInit } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { APP_VERSION } from '../app/version';
import { ThemeService } from './services/theme.service';
import { GameService } from './services/game.service';
import { SteamUserProfile } from './models/game.model';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  standalone: false,
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'Inferno Games';
  version: string = APP_VERSION;
  isDarkMode$: Observable<boolean>;
  private themeSubscription: Subscription = new Subscription();

  webSocketConnected: boolean = true;
  
  // Steam user profile
  steamUser: SteamUserProfile | null = null;
  steamConfigured: boolean = false;

  constructor(
    private themeService: ThemeService,
    private gameService: GameService
  ) {
    this.isDarkMode$ = this.themeService.isDarkMode$;
  }

  ngOnInit(): void {
    // Subscribe to theme changes to ensure DOM classes are applied
    this.themeSubscription = this.themeService.isDarkMode$.subscribe(isDark => {
      document.documentElement.classList.add(isDark ? 'dark-theme' : 'light-theme');
      document.documentElement.classList.remove(isDark ? 'light-theme': 'dark-theme');
    });

    // Load Steam user profile
    this.loadSteamUserProfile();
  }

  ngOnDestroy(): void {
    this.themeSubscription.unsubscribe();
  }

  toggleTheme(): void {
    this.themeService.toggleDarkMode();
  }

  private loadSteamUserProfile(): void {
    this.gameService.getSteamStatus().subscribe(response => {
      this.steamConfigured = response.data?.configured || false;
      
      if (this.steamConfigured) {
        this.gameService.getSteamUserProfile().subscribe(profileResponse => {
          if (profileResponse.data) {
            this.steamUser = profileResponse.data;
          }
        });
      }
    });
  }

  getPersonaStateClass(): string {
    if (!this.steamUser) return 'offline';
    return this.steamUser.personaState === 0 ? 'offline' : 'online';
  }

  getPersonaStateText(): string {
    if (!this.steamUser) return 'Offline';
    return this.steamUser.personaState === 0 ? 'Offline' : 'Online';
  }
}
