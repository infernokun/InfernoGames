import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MaterialModule } from '../../../material.module';
import { ApiResponse } from '../../../models/api-response.model';
import { GamePlatform, GameStatus, Game } from '../../../models/game.model';
import { GameService } from '../../../services/game.service';
import { FADE_IN_UP } from '../../../utils/animations';
import { InfernoGamesHelpers } from '../../../utils/helpers';
@Component({
  selector: 'app-game-form',
  templateUrl: './game-form.component.html',
  styleUrls: ['./game-form.component.scss'],
  imports: [
    CommonModule,
    MaterialModule,
    ReactiveFormsModule,
    RouterModule
  ],
  animations: [FADE_IN_UP]
})
export class GameFormComponent implements OnInit, OnDestroy {
  helpers: typeof InfernoGamesHelpers = InfernoGamesHelpers;

  gameForm!: FormGroup;
  isEditMode = false;
  gameId: number | null = null;
  loading = false;
  saving = false;

  platforms = Object.values(GamePlatform);
  statuses = Object.values(GameStatus);

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private gameService: GameService,
    private snackBar: MatSnackBar
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.gameId = +params['id'];
        this.loadGame();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForm(): void {
    this.gameForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(200)]],
      description: [''],
      developer: [''],
      publisher: [''],
      releaseYear: [null, [Validators.min(1970), Validators.max(2030)]],
      genre: [''],
      coverImageUrl: [''],
      platform: [null],
      status: [GameStatus.NOT_STARTED],
      rating: [null, [Validators.min(1), Validators.max(10)]],
      playtimeHours: [0, [Validators.min(0)]],
      notes: [''],
      favorite: [false],
      isDlc: [false]
    });
  }

  loadGame(): void {
    if (!this.gameId) return;

    this.loading = true;
    this.gameService.getGameById(this.gameId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: ApiResponse<Game>) => {
          if (res.data) {
            this.gameForm.patchValue({
              title: res.data.title,
              description: res.data.description,
              developer: res.data.developer,
              publisher: res.data.publisher,
              releaseYear: res.data.releaseYear,
              genre: res.data.genre,
              coverImageUrl: res.data.coverImageUrl,
              platform: res.data.platform,
              status: res.data.status,
              rating: res.data.rating,
              playtimeHours: res.data.playtimeHours,
              notes: res.data.notes,
              favorite: res.data.favorite,
              dlc: res.data.dlc
            });
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading game:', error);
          this.snackBar.open('Error loading game', 'Close', { duration: 3000 });
          this.loading = false;
        }
      });
  }

  onSubmit(): void {
    if (this.gameForm.invalid) {
      this.gameForm.markAllAsTouched();
      return;
    }

    this.saving = true;
    const gameData = this.gameForm.value;

    if (this.isEditMode && this.gameId) {
      this.gameService.updateGame(this.gameId, gameData)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.snackBar.open('Game updated successfully!', 'Close', { duration: 3000 });
            this.router.navigate(['/games', this.gameId]);
          },
          error: (error) => {
            console.error('Error updating game:', error);
            this.snackBar.open('Error updating game', 'Close', { duration: 3000 });
            this.saving = false;
          }
        });
    } else {
      this.gameService.createGame(gameData)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (res) => {
            this.snackBar.open('Game added successfully!', 'Close', { duration: 3000 });
            if (res.data?.id) {
              this.router.navigate(['/games', res.data.id]);
            } else {
              this.router.navigate(['/games']);
            }
          },
          error: (error) => {
            console.error('Error creating game:', error);
            this.snackBar.open('Error creating game', 'Close', { duration: 3000 });
            this.saving = false;
          }
        });
    }
  }

  cancel(): void {
    if (this.isEditMode && this.gameId) {
      this.router.navigate(['/games', this.gameId]);
    } else {
      this.router.navigate(['/games']);
    }
  }
}