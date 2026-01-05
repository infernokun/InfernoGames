import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { GamesListComponent } from './components/games-list/games-list.component';
import { GameDetailComponent } from './components/game-detail/game-detail.component';
import { GameFormComponent } from './components/game-form/game-form.component';
import { IgdbSearchComponent } from './components/igdb-search/igdb-search.component';

const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'games', component: GamesListComponent },
  { path: 'games/new', component: GameFormComponent },
  { path: 'games/search', component: IgdbSearchComponent },
  { path: 'games/:id', component: GameDetailComponent },
  { path: 'games/:id/edit', component: GameFormComponent },
  { path: '**', redirectTo: '/dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
