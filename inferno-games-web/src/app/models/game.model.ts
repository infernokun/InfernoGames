import { DateUtils } from "../utils/date-utils";

export enum GameStatus {
  NOT_STARTED = 'NOT_STARTED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  ON_HOLD = 'ON_HOLD',
  DROPPED = 'DROPPED'
}

export enum GamePlatform {
  PC = 'PC',
  PLAYSTATION_5 = 'PLAYSTATION_5',
  PLAYSTATION_4 = 'PLAYSTATION_4',
  PLAYSTATION_3 = 'PLAYSTATION_3',
  XBOX_SERIES = 'XBOX_SERIES',
  XBOX_ONE = 'XBOX_ONE',
  XBOX_360 = 'XBOX_360',
  NINTENDO_SWITCH = 'NINTENDO_SWITCH',
  NINTENDO_3DS = 'NINTENDO_3DS',
  STEAM_DECK = 'STEAM_DECK',
  MOBILE_IOS = 'MOBILE_IOS',
  MOBILE_ANDROID = 'MOBILE_ANDROID',
  OTHER = 'OTHER'
}

export interface GameStats {
  totalGames: number;
  completedGames: number;
  inProgressGames: number;
  notStartedGames: number;
  onHoldGames?: number;
  droppedGames?: number;
  favoriteGames?: number;
  totalPlaytime: number;
  averageRating?: number;
  completionRate: number;
  platformBreakdown?: { [key: string]: number };
  genreBreakdown?: { [key: string]: number };
}

export interface PlatformStats {
  platform: GamePlatform;
  gameCount: number;
  completedCount: number;
  percentage: number;
}

export interface GenreStats {
  genre: string;
  gameCount: number;
  percentage: number;
}

// IGDB DTOs
export interface IGDBGame {
  igdbId: number;
  name: string;
  summary?: string;
  storyline?: string;
  coverUrl?: string;
  releaseDate?: Date;
  releaseYear?: number;
  developer?: string;
  publisher?: string;
  genres?: string[];
  platforms?: string[];
  rating?: number;
  ratingCount?: number;
  aggregatedRating?: number;
  url?: string;
  screenshotUrls?: string[];
  steamAppId?: string;
}

// Steam DTOs
export interface SteamGameInfo {
  appId: string;
  name: string;
  playtimeForever: number;        // Total playtime in minutes
  playtimeWindowsForever: number; // Windows playtime in minutes
  playtimeMacForever: number;     // Mac playtime in minutes
  playtimeLinuxForever: number;   // Linux playtime in minutes
  playtimeDeckForever: number;    // Steam Deck playtime in minutes
  imgIconUrl?: string;
  hasCommunityVisibleStats: boolean;
  rtimeLastPlayed: number;        // Unix timestamp
  playtimeDisconnected: number;   // Offline playtime in minutes

  // Enrichment fields (populated by with-genres endpoint)
  genres?: string[];
  inBacklog?: boolean;
  backlogGameId?: number;
}

export interface SteamPlaytimeInfo {
  appId: string;
  playtimeForeverMinutes: number;
  playtimeForeverHours: number;
  playtimeWindowsMinutes: number;
  playtimeMacMinutes: number;
  playtimeLinuxMinutes: number;
  playtimeDeckMinutes: number;
  playtimeDisconnectedMinutes: number;
  lastPlayed?: Date;
}

export interface SteamLibraryStats {
  totalGames: number;
  playedGames: number;
  unplayedGames: number;
  totalPlaytimeMinutes: number;
  totalPlaytimeHours: number;
  deckPlaytimeMinutes: number;
  deckPlaytimeHours: number;
  windowsPlaytimeMinutes: number;
  windowsPlaytimeHours: number;
  linuxPlaytimeMinutes: number;
  linuxPlaytimeHours: number;
  playedPercentage: number;
}

export interface SteamStatus {
  configured: boolean;
  message: string;
}

export interface SteamUserProfile {
  steamId: string;
  personaName: string;
  profileUrl: string;
  avatar: string;           // 32x32
  avatarMedium: string;     // 64x64
  avatarFull: string;       // 184x184
  avatarHash: string;
  personaState: number;     // 0=Offline, 1=Online, 2=Busy, 3=Away, 4=Snooze, 5=Looking to trade, 6=Looking to play
  communityVisibilityState: number;
  profileState: number;
  lastLogoff: number;
  realName?: string;
  countryCode?: string;
  stateCode?: string;
  cityId?: number;
  timeCreated: number;
}

export class Game {
  id?: number;
  title?: string;
  description?: string;
  developer?: string;
  publisher?: string;
  releaseYear?: number;
  releaseDate?: Date;
  genre?: string;
  genres?: string[];
  coverImageUrl?: string;
  screenshotUrls?: string[];
  platform?: GamePlatform | string;
  platforms?: GamePlatform[];
  status?: GameStatus;
  rating?: number; // 1-10
  playtimeHours?: number;
  completionPercentage?: number;
  startedAt?: Date;
  completedAt?: Date;
  notes?: string;
  favorite?: boolean;
  dlcOwned?: string[];
  achievements?: number;
  totalAchievements?: number;

  // IGDB Integration fields
  igdbId?: number;
  igdbUrl?: string;
  igdbRating?: number;
  igdbRatingCount?: number;

  // Steam Integration fields
  steamAppId?: string;
  steamPlaytimeWindowsMinutes?: number;
  steamPlaytimeLinuxMinutes?: number;
  steamPlaytimeMacMinutes?: number;
  steamPlaytimeDeckMinutes?: number;
  steamLastPlayed?: Date;
  steamLastSynced?: Date;

  createdAt?: Date;
  updatedAt?: Date;

  constructor(data?: any) {
    if (data) {
      this.id = data.id;
      this.title = data.title;
      this.description = data.description;
      this.developer = data.developer;
      this.publisher = data.publisher;
      this.releaseYear = data.releaseYear;
      this.releaseDate = data.releaseDate ? DateUtils.parseDateTimeArray(data.releaseDate) : undefined;
      this.genre = data.genre;
      this.genres = data.genres || [];
      this.coverImageUrl = data.coverImageUrl;
      this.screenshotUrls = data.screenshotUrls || [];
      this.platform = data.platform;
      this.platforms = data.platforms || [];
      this.status = data.status || GameStatus.NOT_STARTED;
      this.rating = data.rating;
      this.playtimeHours = data.playtimeHours || 0;
      this.completionPercentage = data.completionPercentage || 0;
      this.startedAt = data.startedAt ? DateUtils.parseDateTimeArray(data.startedAt) : undefined;
      this.completedAt = data.completedAt ? DateUtils.parseDateTimeArray(data.completedAt) : undefined;
      this.notes = data.notes;
      this.favorite = data.favorite || false;
      this.dlcOwned = data.dlcOwned || [];
      this.achievements = data.achievements || 0;
      this.totalAchievements = data.totalAchievements || 0;

      // IGDB fields
      this.igdbId = data.igdbId;
      this.igdbUrl = data.igdbUrl;
      this.igdbRating = data.igdbRating;
      this.igdbRatingCount = data.igdbRatingCount;

      // Steam fields
      this.steamAppId = data.steamAppId;
      this.steamPlaytimeWindowsMinutes = data.steamPlaytimeWindowsMinutes;
      this.steamPlaytimeLinuxMinutes = data.steamPlaytimeLinuxMinutes;
      this.steamPlaytimeMacMinutes = data.steamPlaytimeMacMinutes;
      this.steamPlaytimeDeckMinutes = data.steamPlaytimeDeckMinutes;
      this.steamLastPlayed = data.steamLastPlayed ? DateUtils.parseDateTimeArray(data.steamLastPlayed) : undefined;
      this.steamLastSynced = data.steamLastSynced ? DateUtils.parseDateTimeArray(data.steamLastSynced) : undefined;

      this.createdAt = data.createdAt ? DateUtils.parseDateTimeArray(data.createdAt) : undefined;
      this.updatedAt = data.updatedAt ? DateUtils.parseDateTimeArray(data.updatedAt) : undefined;
    }
  }

  // Helper methods for Steam playtime
  get hasSteamData(): boolean {
    return !!this.steamAppId;
  }

  get steamTotalPlaytimeMinutes(): number {
    return (this.steamPlaytimeWindowsMinutes || 0) +
      (this.steamPlaytimeLinuxMinutes || 0) +
      (this.steamPlaytimeMacMinutes || 0);
  }

  getSteamPlaytimeBreakdown(): { platform: string; minutes: number; hours: number; percentage: number }[] {
    const total = this.steamTotalPlaytimeMinutes || 1; // Avoid division by zero
    const breakdown = [];

    if (this.steamPlaytimeWindowsMinutes && this.steamPlaytimeWindowsMinutes > 0) {
      breakdown.push({
        platform: 'Windows',
        minutes: this.steamPlaytimeWindowsMinutes,
        hours: Math.round(this.steamPlaytimeWindowsMinutes / 60 * 10) / 10,
        percentage: Math.round(this.steamPlaytimeWindowsMinutes / total * 100)
      });
    }

    /*if (this.steamPlaytimeLinuxMinutes && this.steamPlaytimeLinuxMinutes > 0) {
      breakdown.push({
        platform: 'Linux',
        minutes: this.steamPlaytimeLinuxMinutes,
        hours: Math.round(this.steamPlaytimeLinuxMinutes / 60 * 10) / 10,
        percentage: Math.round(this.steamPlaytimeLinuxMinutes / total * 100)
      });
    }*/

    /*if (this.steamPlaytimeMacMinutes && this.steamPlaytimeMacMinutes > 0) {
      breakdown.push({
        platform: 'Mac',
        minutes: this.steamPlaytimeMacMinutes,
        hours: Math.round(this.steamPlaytimeMacMinutes / 60 * 10) / 10,
        percentage: Math.round(this.steamPlaytimeMacMinutes / total * 100)
      });
    }*/

    if (this.steamPlaytimeDeckMinutes && this.steamPlaytimeDeckMinutes > 0) {
      breakdown.push({
        platform: 'Steam Deck',
        minutes: this.steamPlaytimeDeckMinutes,
        hours: Math.round(this.steamPlaytimeDeckMinutes / 60 * 10) / 10,
        percentage: Math.round(this.steamPlaytimeDeckMinutes / total * 100)
      });
    }

    return breakdown.sort((a, b) => b.minutes - a.minutes);
  }
}
