import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { errorMessage } from '../../../core/api-error';
import { AuthService } from '../../../core/auth-service';
import { CommentService } from '../../../core/comment-service';
import { Comment, Page } from '../../../core/models';
import { Avatar } from '../../../shared/avatar/avatar';
import { Pagination } from '../../../shared/pagination/pagination';

/**
 * The discussion under an article: listing, posting, editing and deleting.
 *
 * The permission rules mirror the server's, which stays the authority - these
 * checks only decide whether a button is worth showing.
 */
@Component({
  selector: 'app-comments',
  imports: [FormsModule, RouterLink, DatePipe, Avatar, Pagination],
  templateUrl: './comments.html',
  styleUrl: './comments.scss',
})
export class Comments {
  readonly slug = input.required<string>();
  /** Lets the article's author moderate the discussion on their own page. */
  readonly articleAuthorId = input.required<number>();

  readonly countChange = output<number>();

  private readonly commentService = inject(CommentService);
  protected readonly auth = inject(AuthService);

  protected readonly page = signal<Page<Comment> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly posting = signal(false);

  protected newComment = '';

  /** The comment being edited, and the text as it is being changed. */
  protected readonly editingId = signal<number | null>(null);
  protected editingText = '';

  constructor() {
    // Reacts to the slug so that navigating between articles reloads the thread.
    effect(() => {
      const slug = this.slug();
      this.load(slug, 0);
    });
  }

  private load(slug: string, page: number): void {
    this.loading.set(true);
    this.commentService.list(slug, page).subscribe({
      next: (result) => {
        this.page.set(result);
        this.loading.set(false);
        this.countChange.emit(result.totalElements);
      },
      error: (failure: unknown) => {
        this.loading.set(false);
        this.error.set(errorMessage(failure, 'Could not load the comments'));
      },
    });
  }

  protected goToPage(page: number): void {
    this.load(this.slug(), page);
  }

  protected submit(): void {
    const content = this.newComment.trim();
    if (content === '' || this.posting()) {
      return;
    }

    this.posting.set(true);
    this.error.set(null);

    this.commentService.add(this.slug(), { content }).subscribe({
      next: () => {
        this.newComment = '';
        this.posting.set(false);
        // The thread runs oldest first, so a new comment lands on the last page.
        this.load(this.slug(), this.lastPageIndex());
      },
      error: (failure: unknown) => {
        this.posting.set(false);
        this.error.set(errorMessage(failure, 'Could not post the comment'));
      },
    });
  }

  /** A new comment lands at the end of the thread, so jump there to show it. */
  private lastPageIndex(): number {
    const current = this.page();
    if (!current) {
      return 0;
    }
    const total = current.totalElements + 1;
    return Math.max(0, Math.ceil(total / current.size) - 1);
  }

  protected startEditing(comment: Comment): void {
    this.editingId.set(comment.id);
    this.editingText = comment.content;
  }

  protected cancelEditing(): void {
    this.editingId.set(null);
    this.editingText = '';
  }

  protected saveEditing(comment: Comment): void {
    const content = this.editingText.trim();
    if (content === '' || content === comment.content) {
      this.cancelEditing();
      return;
    }

    this.commentService.update(comment.id, { content }).subscribe({
      next: (updated) => {
        this.replace(updated);
        this.cancelEditing();
      },
      error: (failure: unknown) =>
        this.error.set(errorMessage(failure, 'Could not save the comment')),
    });
  }

  protected remove(comment: Comment): void {
    if (!confirm('Delete this comment?')) {
      return;
    }
    this.commentService.remove(comment.id).subscribe({
      next: () => this.load(this.slug(), this.page()?.page ?? 0),
      error: (failure: unknown) =>
        this.error.set(errorMessage(failure, 'Could not delete the comment')),
    });
  }

  /** Only the person who wrote a comment may change its words. */
  protected canEdit(comment: Comment): boolean {
    return this.auth.currentUser()?.id === comment.author.id;
  }

  /** The author of the comment, the author of the article, or an administrator. */
  protected canDelete(comment: Comment): boolean {
    const me = this.auth.currentUser();
    if (!me) {
      return false;
    }
    return me.id === comment.author.id || me.id === this.articleAuthorId() || me.role === 'ADMIN';
  }

  private replace(updated: Comment): void {
    const current = this.page();
    if (!current) {
      return;
    }
    this.page.set({
      ...current,
      content: current.content.map((item) => (item.id === updated.id ? updated : item)),
    });
  }
}
