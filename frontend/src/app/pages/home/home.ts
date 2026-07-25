import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { errorMessage } from '../../core/api-error';
import { ArticleService } from '../../core/article-service';
import { AuthService } from '../../core/auth-service';
import { ArticleSort, ArticleSummary, Category, Page, Tag } from '../../core/models';
import { TaxonomyService } from '../../core/taxonomy-service';
import { ArticleCard } from '../../shared/article-card/article-card';
import { Pagination } from '../../shared/pagination/pagination';

/**
 * The feed of published articles.
 *
 * All of the state - the search text, the filters, the sort and the page - lives
 * in the URL rather than in the component. That makes a filtered list something
 * you can bookmark or send to somebody, and it makes the browser's back button
 * behave the way a reader expects.
 */
@Component({
  selector: 'app-home',
  imports: [RouterLink, FormsModule, ArticleCard, Pagination],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private readonly articleService = inject(ArticleService);
  private readonly taxonomy = inject(TaxonomyService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly auth = inject(AuthService);

  protected readonly page = signal<Page<ArticleSummary> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly categories = signal<Category[]>([]);
  protected readonly tags = signal<Tag[]>([]);

  /** The active filters, read back out of the URL. */
  protected readonly query = signal('');
  protected readonly category = signal<string | null>(null);
  protected readonly tag = signal<string | null>(null);
  protected readonly author = signal<string | null>(null);
  protected readonly sortBy = signal<ArticleSort>('RECENT');

  /** What the reader is typing, before they submit it. */
  protected searchText = '';

  protected readonly hasFilters = computed(
    () => this.query() !== '' || !!this.category() || !!this.tag() || !!this.author(),
  );

  constructor() {
    this.taxonomy.categories().pipe(takeUntilDestroyed()).subscribe({
      next: (categories) => this.categories.set(categories),
      // The filter bar is a convenience; if it cannot load, the feed still works.
      error: () => undefined,
    });
    this.taxonomy.tags().pipe(takeUntilDestroyed()).subscribe({
      next: (tags) => this.tags.set(tags),
      error: () => undefined,
    });

    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      this.query.set(params.get('query') ?? '');
      this.searchText = this.query();
      this.category.set(params.get('category'));
      this.tag.set(params.get('tag'));
      this.author.set(params.get('author'));
      this.sortBy.set((params.get('sortBy') as ArticleSort | null) ?? 'RECENT');
      this.load(Number(params.get('page') ?? 0));
    });
  }

  private load(page: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.articleService
      .list({
        query: this.query() || undefined,
        category: this.category() ?? undefined,
        tag: this.tag() ?? undefined,
        author: this.author() ?? undefined,
        sortBy: this.sortBy(),
        page,
        size: 9,
      })
      .subscribe({
        next: (result) => {
          this.page.set(result);
          this.loading.set(false);
        },
        error: (failure: unknown) => {
          this.loading.set(false);
          this.error.set(errorMessage(failure, 'Could not load the articles'));
        },
      });
  }

  /** Changing any filter resets to the first page: page 4 of the old result is meaningless. */
  protected applyFilters(changes: Record<string, string | null>): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ...changes, page: null },
      queryParamsHandling: 'merge',
    });
  }

  protected search(): void {
    this.applyFilters({ query: this.searchText.trim() || null });
  }

  protected clearFilters(): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  protected selectCategory(slug: string | null): void {
    this.applyFilters({ category: this.category() === slug ? null : slug });
  }

  protected selectTag(slug: string | null): void {
    this.applyFilters({ tag: this.tag() === slug ? null : slug });
  }

  protected selectSort(sort: ArticleSort): void {
    this.applyFilters({ sortBy: sort === 'RECENT' ? null : sort });
  }

  protected goToPage(page: number): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: page === 0 ? null : page },
      queryParamsHandling: 'merge',
    });
  }
}
