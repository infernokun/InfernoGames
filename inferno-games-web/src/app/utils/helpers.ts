import { GameStatus } from "../models/game.model";

export class InfernoGamesHelpers {
    public static getPlatformLabel(platform: string): string {
        switch (platform) {
            case 'PC': return 'PC';
            case 'PLAYSTATION_5': return 'PlayStation 5';
            case 'PLAYSTATION_4': return 'PlayStation 4';
            case 'XBOX_SERIES': return 'Xbox Series X|S';
            case 'XBOX_ONE': return 'Xbox One';
            case 'NINTENDO_SWITCH': return 'Nintendo Switch';
            case 'STEAM_DECK': return 'Steam Deck';
            case 'MOBILE': return 'Mobile';
            default: return 'Other';
        }
    }

    public static getStatusColor(status: GameStatus | undefined): string {
        switch (status) {
            case GameStatus.COMPLETED: return 'completed';
            case GameStatus.IN_PROGRESS: return 'in-progress';
            case GameStatus.ON_HOLD: return 'on-hold';
            case GameStatus.DROPPED: return 'dropped';
            default: return 'not-started';
        }
    }

    public static getStatusLabel(status: GameStatus | undefined): string {
        switch (status) {
            case GameStatus.COMPLETED: return 'Completed';
            case GameStatus.IN_PROGRESS: return 'Playing';
            case GameStatus.ON_HOLD: return 'On Hold';
            case GameStatus.DROPPED: return 'Dropped';
            default: return 'Backlog';
        }
    }

    public static getStatusTooltip(status: GameStatus | undefined): string {
        switch (status) {
            case GameStatus.COMPLETED: return 'You\'ve finished this game';
            case GameStatus.IN_PROGRESS: return 'Currently playing';
            case GameStatus.ON_HOLD: return 'Taking a break from this one';
            case GameStatus.DROPPED: return 'No longer playing';
            default: return 'Haven\'t started yet';
        }
    }

    public static getPlatformIcon(platform: string): string {
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
}