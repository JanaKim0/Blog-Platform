import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { errorMessage } from '../../core/api-error';
import { ArticleService } from '../../core/article-service';
import { AuthService } from '../../core/auth-service';
import { Article as ArticleModel } from '../../core/models';
import { Avatar } from '../../shared/avatar/avatar';
import { Comments } from './comments/comments';

@Component({
  selector: 'app-article',
  imports: [RouterLink, DatePipe, Avatar, Comments],
  templateUrl: './article.html',
  styleUrl: './article.scss',
})
export class Article {
  private readonly articleService = inject(ArticleService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly pageTitle = inject(Title);

  protected readonly auth = inject(AuthService);

  protected readonly article = signal<ArticleModel | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notFound = signal(false);
  /** Keeps the heart from firing twice while the first request is in flight. */
  protected readonly liking = signal(false);

  constructor() {
    // Reads the slug from the route rather than once on creation, so following a
    // link from one article to another reloads the content.
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const slug = params.get('slug');
      if (slug) {
        this.load(slug);
      }
    });
  }

  private load(slug: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.notFound.set(false);

    this.articleService.get(slug).subscribe({
      next: (article) => {
        this.article.set(article);
        this.loading.set(false);
        // The route's generic title is only a placeholder until the real one is known.
        this.pageTitle.setTitle(`${article.title} · Blog Platform`);
      },
      error: (failure: unknown) => {
        this.loading.set(false);
        // A draft belonging to somebody else also answers 404 - by design, so
        // that its existence is not revealed.
        if (typeof failure === 'object' && failure !== null && 'status' in failure
          && (failure as { status: number }).status === 404) {
          this.notFound.set(true);
        } else {
          this.error.set(errorMessage(failure, 'Could not load the article'));
        }
      },
    });
  }

  protected toggleLike(): void {
    const current = this.article();
    if (!current || this.liking()) {
      return;
    }
    if (!this.auth.isSignedIn()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }

    this.liking.set(true);
    const request = current.likedByMe
      ? this.articleService.unlike(current.slug)
      : this.articleService.like(current.slug);

    request.subscribe({
      // The endpoint answers with the whole article, so the new count arrives
      // with it and there is nothing to guess at on the client.
      next: (updated) => {
        this.article.set(updated);
        this.liking.set(false);
      },
      error: (failure: unknown) => {
        this.liking.set(false);
        this.error.set(errorMessage(failure, 'Could not register your like'));
      },
    });
  }

  /** Keeps the comment count in the header honest when a comment is added or removed. */
  protected onCommentCountChange(count: number): void {
    const current = this.article();
    if (current) {
      this.article.set({ ...current, commentsCount: count });
    }
  }
}
