import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { toHttpParams } from './http-params';
import { Article, ArticleQuery, ArticleRequest, ArticleSummary, Page } from './models';

@Injectable({ providedIn: 'root' })
export class ArticleService {
  private readonly http = inject(HttpClient);

  /** The public list of published articles, with every filter optional. */
  list(query: ArticleQuery): Observable<Page<ArticleSummary>> {
    return this.http.get<Page<ArticleSummary>>('/api/articles', {
      params: toHttpParams({ ...query }),
    });
  }

  /** Articles by the authors the signed-in reader follows. */
  feed(page = 0, size = 10): Observable<Page<ArticleSummary>> {
    return this.http.get<Page<ArticleSummary>>('/api/me/feed', {
      params: toHttpParams({ page, size }),
    });
  }

  /** The signed-in author's own articles, drafts included. */
  mine(page = 0, size = 20): Observable<Page<ArticleSummary>> {
    return this.http.get<Page<ArticleSummary>>('/api/me/articles', {
      params: toHttpParams({ page, size }),
    });
  }

  get(slug: string): Observable<Article> {
    return this.http.get<Article>(`/api/articles/${encodeURIComponent(slug)}`);
  }

  create(request: ArticleRequest): Observable<Article> {
    return this.http.post<Article>('/api/articles', request);
  }

  update(slug: string, request: ArticleRequest): Observable<Article> {
    return this.http.put<Article>(`/api/articles/${encodeURIComponent(slug)}`, request);
  }

  remove(slug: string): Observable<void> {
    return this.http.delete<void>(`/api/articles/${encodeURIComponent(slug)}`);
  }

  uploadCover(slug: string, file: File): Observable<Article> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Article>(`/api/articles/${encodeURIComponent(slug)}/cover`, form);
  }

  removeCover(slug: string): Observable<Article> {
    return this.http.delete<Article>(`/api/articles/${encodeURIComponent(slug)}/cover`);
  }

  /** Both answer with the whole article, so the new count arrives with it. */
  like(slug: string): Observable<Article> {
    return this.http.post<Article>(`/api/articles/${encodeURIComponent(slug)}/like`, {});
  }

  unlike(slug: string): Observable<Article> {
    return this.http.delete<Article>(`/api/articles/${encodeURIComponent(slug)}/like`);
  }
}
