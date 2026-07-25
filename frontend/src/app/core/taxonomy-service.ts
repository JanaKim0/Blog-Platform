import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';

import { Category, Tag } from './models';

/**
 * Categories and tags. Both lists change rarely and are needed by the feed
 * filters and by the editor, so they are fetched once and shared.
 */
@Injectable({ providedIn: 'root' })
export class TaxonomyService {
  private readonly http = inject(HttpClient);

  private categories$?: Observable<Category[]>;
  private tags$?: Observable<Tag[]>;

  categories(): Observable<Category[]> {
    this.categories$ ??= this.http
      .get<Category[]>('/api/categories')
      .pipe(shareReplay({ bufferSize: 1, refCount: false }));
    return this.categories$;
  }

  tags(): Observable<Tag[]> {
    this.tags$ ??= this.http
      .get<Tag[]>('/api/tags')
      .pipe(shareReplay({ bufferSize: 1, refCount: false }));
    return this.tags$;
  }

  /** Called after an article introduces new tags, so the filter list catches up. */
  invalidateTags(): void {
    this.tags$ = undefined;
  }
}
