import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { combineLatest } from 'rxjs';

import { errorMessage } from '../../core/api-error';
import { Page, UserSummary } from '../../core/models';
import { UserService } from '../../core/user-service';
import { Pagination } from '../../shared/pagination/pagination';
import { UserCard } from '../../shared/user-card/user-card';

type Mode = 'followers' | 'following';

/**
 * "Who follows this author" and "whom they follow". The two lists differ only in
 * which endpoint they call, so the route says which one it is through its data.
 */
@Component({
  selector: 'app-connections',
  imports: [RouterLink, UserCard, Pagination],
  templateUrl: './connections.html',
  styleUrl: './connections.scss',
})
export class Connections {
  private readonly users = inject(UserService);
  private readonly route = inject(ActivatedRoute);

  protected readonly username = signal('');
  protected readonly mode = signal<Mode>('followers');
  protected readonly page = signal<Page<UserSummary> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly heading = computed(() =>
    this.mode() === 'followers' ? 'Followers' : 'Following',
  );

  constructor() {
    combineLatest([this.route.paramMap, this.route.data])
      .pipe(takeUntilDestroyed())
      .subscribe(([params, data]) => {
        this.username.set(params.get('username') ?? '');
        this.mode.set((data['mode'] as Mode) ?? 'followers');
        this.load(0);
      });
  }

  protected load(page: number): void {
    this.loading.set(true);
    this.error.set(null);

    const request =
      this.mode() === 'followers'
        ? this.users.followers(this.username(), page)
        : this.users.following(this.username(), page);

    request.subscribe({
      next: (result) => {
        this.page.set(result);
        this.loading.set(false);
      },
      error: (failure: unknown) => {
        this.loading.set(false);
        this.error.set(errorMessage(failure, 'Could not load the list'));
      },
    });
  }
}
