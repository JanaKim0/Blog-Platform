import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { errorMessage } from '../../core/api-error';
import { ArticleService } from '../../core/article-service';
import { AuthService } from '../../core/auth-service';
import { ArticleSummary, Page, Profile as ProfileModel } from '../../core/models';
import { UserService } from '../../core/user-service';
import { ArticleCard } from '../../shared/article-card/article-card';
import { Avatar } from '../../shared/avatar/avatar';
import { Pagination } from '../../shared/pagination/pagination';

/** An author's public page: who they are, and everything they have published. */
@Component({
  selector: 'app-profile',
  imports: [RouterLink, DatePipe, Avatar, ArticleCard, Pagination],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile {
  private readonly users = inject(UserService);
  private readonly articles = inject(ArticleService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly pageTitle = inject(Title);

  protected readonly auth = inject(AuthService);

  protected readonly profile = signal<ProfileModel | null>(null);
  protected readonly articlePage = signal<Page<ArticleSummary> | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly followBusy = signal(false);

  /** One's own profile is edited, not followed. */
  protected readonly isMe = computed(
    () => this.auth.currentUser()?.id === this.profile()?.id,
  );

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const username = params.get('username');
      if (username) {
        this.load(username);
      }
    });
  }

  private load(username: string): void {
    this.loading.set(true);
    this.notFound.set(false);
    this.error.set(null);

    this.users.profile(username).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.loading.set(false);
        this.pageTitle.setTitle(`${profile.displayName} · Blog Platform`);
        this.loadArticles(username, 0);
      },
      error: (failure: unknown) => {
        this.loading.set(false);
        if (
          typeof failure === 'object' && failure !== null && 'status' in failure &&
          (failure as { status: number }).status === 404
        ) {
          this.notFound.set(true);
        } else {
          this.error.set(errorMessage(failure, 'Could not load the profile'));
        }
      },
    });
  }

  protected loadArticles(username: string, page: number): void {
    this.articles.list({ author: username, page, size: 6 }).subscribe({
      next: (result) => this.articlePage.set(result),
      error: () => this.error.set('Could not load this author’s articles'),
    });
  }

  protected onArticlePage(page: number): void {
    const username = this.profile()?.username;
    if (username) {
      this.loadArticles(username, page);
    }
  }

  protected toggleFollow(): void {
    const profile = this.profile();
    if (!profile || this.followBusy()) {
      return;
    }
    if (!this.auth.isSignedIn()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }

    this.followBusy.set(true);
    const request = profile.following
      ? this.users.unfollow(profile.username)
      : this.users.follow(profile.username);

    request.subscribe({
      // The endpoint answers with the profile, so the follower count comes back
      // with it rather than being adjusted by hand here.
      next: (updated) => {
        this.profile.set(updated);
        this.followBusy.set(false);
      },
      error: (failure: unknown) => {
        this.followBusy.set(false);
        this.error.set(errorMessage(failure, 'Could not change your subscription'));
      },
    });
  }
}
