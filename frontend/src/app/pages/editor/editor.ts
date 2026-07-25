import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { errorMessage, fieldErrors } from '../../core/api-error';
import { ArticleService } from '../../core/article-service';
import { Article, ArticleStatus, Category, Tag } from '../../core/models';
import { formatTagInput, parseTagInput } from '../../core/tags';
import { TaxonomyService } from '../../core/taxonomy-service';

/**
 * Writing and editing an article. One component serves both: the difference is
 * only whether a slug was in the route.
 */
@Component({
  selector: 'app-editor',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './editor.html',
  styleUrl: './editor.scss',
})
export class Editor {
  private readonly articleService = inject(ArticleService);
  private readonly taxonomy = inject(TaxonomyService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly slug = signal<string | null>(null);
  protected readonly editing = computed(() => this.slug() !== null);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverFieldErrors = signal<Record<string, string>>({});

  protected readonly categories = signal<Category[]>([]);
  protected readonly tagSuggestions = signal<Tag[]>([]);

  /** The cover already stored on the server, if any. */
  protected readonly coverUrl = signal<string | null>(null);
  /** A chosen file that has not been uploaded yet, and its local preview. */
  private readonly pendingCover = signal<File | null>(null);
  protected readonly pendingPreview = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    summary: ['', Validators.maxLength(500)],
    content: ['', Validators.required],
    categorySlug: [''],
    tags: [''],
    status: ['DRAFT' as ArticleStatus, Validators.required],
  });

  constructor() {
    this.taxonomy.categories().pipe(takeUntilDestroyed()).subscribe({
      next: (categories) => this.categories.set(categories),
      error: () => undefined,
    });
    this.taxonomy.tags().pipe(takeUntilDestroyed()).subscribe({
      next: (tags) => this.tagSuggestions.set(tags),
      error: () => undefined,
    });

    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const slug = params.get('slug');
      this.slug.set(slug);
      if (slug) {
        this.load(slug);
      }
    });

    // An object URL stays allocated until it is revoked, so it is released when
    // the editor goes away.
    this.destroyRef.onDestroy(() => this.releasePreview());
  }

  private load(slug: string): void {
    this.loading.set(true);
    this.articleService.get(slug).subscribe({
      next: (article) => {
        this.fill(article);
        this.loading.set(false);
      },
      error: (failure: unknown) => {
        this.loading.set(false);
        this.error.set(errorMessage(failure, 'Could not open the article'));
      },
    });
  }

  private fill(article: Article): void {
    this.form.setValue({
      title: article.title,
      summary: article.summary ?? '',
      content: article.content,
      categorySlug: article.category?.slug ?? '',
      tags: formatTagInput(article.tags.map((tag) => tag.name)),
      status: article.status,
    });
    this.coverUrl.set(article.coverUrl);
  }

  protected onCoverSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.releasePreview();

    if (file) {
      this.pendingCover.set(file);
      this.pendingPreview.set(URL.createObjectURL(file));
    } else {
      this.pendingCover.set(null);
    }
  }

  protected save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.serverFieldErrors.set({});

    const raw = this.form.getRawValue();
    const payload = {
      title: raw.title.trim(),
      summary: raw.summary.trim() === '' ? null : raw.summary.trim(),
      content: raw.content,
      categorySlug: raw.categorySlug === '' ? null : raw.categorySlug,
      tags: parseTagInput(raw.tags),
      status: raw.status,
    };

    const slug = this.slug();
    const request = slug
      ? this.articleService.update(slug, payload)
      : this.articleService.create(payload);

    request.subscribe({
      next: (article) => this.afterSave(article),
      error: (failure: unknown) => {
        this.saving.set(false);
        this.error.set(errorMessage(failure, 'Could not save the article'));
        this.serverFieldErrors.set(fieldErrors(failure));
      },
    });
  }

  /**
   * The cover can only be uploaded once the article exists, so a file chosen
   * while writing a new post is sent straight after it is created.
   */
  private afterSave(article: Article): void {
    const file = this.pendingCover();
    if (!file) {
      this.finish(article);
      return;
    }

    this.articleService.uploadCover(article.slug, file).subscribe({
      next: (withCover) => this.finish(withCover),
      error: (failure: unknown) => {
        // The text is saved; only the image failed, and saying so is more useful
        // than pretending the whole save went wrong.
        this.saving.set(false);
        this.slug.set(article.slug);
        this.coverUrl.set(article.coverUrl);
        this.pendingCover.set(null);
        this.releasePreview();
        this.error.set(
          errorMessage(failure, 'The article was saved, but the cover image was not uploaded'),
        );
      },
    });
  }

  private finish(article: Article): void {
    this.saving.set(false);
    this.pendingCover.set(null);
    this.releasePreview();
    // The article may have introduced tags nobody had used before, and the tag
    // list is cached for the whole session.
    this.taxonomy.invalidateTags();

    // A published article is worth looking at; a draft goes back to the list,
    // where the author can see it next to the rest.
    if (article.status === 'PUBLISHED') {
      this.router.navigate(['/articles', article.slug]);
    } else {
      this.router.navigate(['/my-articles']);
    }
  }

  protected removeCover(): void {
    const slug = this.slug();
    if (!slug || !this.coverUrl()) {
      return;
    }
    if (!confirm('Remove the cover image?')) {
      return;
    }
    this.articleService.removeCover(slug).subscribe({
      next: (article) => this.coverUrl.set(article.coverUrl),
      error: (failure: unknown) =>
        this.error.set(errorMessage(failure, 'Could not remove the cover image')),
    });
  }

  protected remove(): void {
    const slug = this.slug();
    if (!slug) {
      return;
    }
    if (!confirm('Delete this article? Its comments and likes go with it.')) {
      return;
    }
    this.articleService.remove(slug).subscribe({
      next: () => this.router.navigate(['/my-articles']),
      error: (failure: unknown) =>
        this.error.set(errorMessage(failure, 'Could not delete the article')),
    });
  }

  private releasePreview(): void {
    const preview = this.pendingPreview();
    if (preview) {
      URL.revokeObjectURL(preview);
      this.pendingPreview.set(null);
    }
  }
}
