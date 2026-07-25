import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { errorMessage } from '../../core/api-error';
import { Page, UserSummary } from '../../core/models';
import { UserService } from '../../core/user-service';
import { Pagination } from '../../shared/pagination/pagination';
import { UserCard } from '../../shared/user-card/user-card';

/**
 * Finding people. Like the article feed, the query lives in the URL, so a search
 * can be shared and the back button works.
 */
@Component({
  selector: 'app-people',
  imports: [FormsModule, UserCard, Pagination],
  templateUrl: './people.html',
  styleUrl: './people.scss',
})
export class People {
  private readonly users = inject(UserService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly page = signal<Page<UserSummary> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly query = signal('');

  protected searchText = '';

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      this.query.set(params.get('query') ?? '');
      this.searchText = this.query();
      this.load(Number(params.get('page') ?? 0));
    });
  }

  private load(page: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.users.search(this.query(), page, 24).subscribe({
      next: (result) => {
        this.page.set(result);
        this.loading.set(false);
      },
      error: (failure: unknown) => {
        this.loading.set(false);
        this.error.set(errorMessage(failure, 'Could not load people'));
      },
    });
  }

  protected search(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { query: this.searchText.trim() || null, page: null },
    });
  }

  protected goToPage(page: number): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: page === 0 ? null : page },
      queryParamsHandling: 'merge',
    });
  }
}
