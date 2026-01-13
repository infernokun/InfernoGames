import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { GamesListComponent } from './components/games/list/games-list.component';
import { GameDetailComponent } from './components/games/detail/game-detail.component';
import { GameFormComponent } from './components/games/form/game-form.component';
import { IgdbSearchComponent } from './components/igdb/search/igdb-search.component';
import { SteamLibraryComponent } from './components/steam/library/steam-library.component';
import { SteamStatsComponent } from './components/steam/stats/steam-stats.component';

const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'games', component: GamesListComponent },
  { path: 'games/new', component: GameFormComponent },
  { path: 'games/search', component: IgdbSearchComponent },
  { path: 'games/:id', component: GameDetailComponent },
  { path: 'games/:id/edit', component: GameFormComponent },
  { path: 'steam/library', component: SteamLibraryComponent },
  { path: 'steam/stats', component: SteamStatsComponent },
  { path: '**', redirectTo: '/dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
