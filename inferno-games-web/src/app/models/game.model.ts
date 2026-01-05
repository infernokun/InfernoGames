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
      
      this.createdAt = data.createdAt ? DateUtils.parseDateTimeArray(data.createdAt) : undefined;
      this.updatedAt = data.updatedAt ? DateUtils.parseDateTimeArray(data.updatedAt) : undefined;
    }
  }
}
