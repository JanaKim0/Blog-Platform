import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { errorMessage } from '../../core/api-error';
import { ArticleService } from '../../core/article-service';
import { ArticleSummary, Page } from '../../core/models';
import { ArticleCard } from '../../shared/article-card/article-card';
import { Pagination } from '../../shared/pagination/pagination';

/** The author's own articles, drafts included, newest change first. */
@Component({
  selector: 'app-my-articles',
  imports: [RouterLink, ArticleCard, Pagination],
  templateUrl: './my-articles.html',
  styleUrl: './my-articles.scss',
})
export class MyArticles {
  private readonly articleService = inject(ArticleService);

  protected readonly page = signal<Page<ArticleSummary> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.load(0);
  }

  protected load(page: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.articleService.mine(page, 9).subscribe({
      next: (result) => {
        this.page.set(result);
        this.loading.set(false);
      },
      error: (failure: unknown) => {
        this.loading.set(false);
        this.error.set(errorMessage(failure, 'Could not load your articles'));
      },
    });
  }
}
