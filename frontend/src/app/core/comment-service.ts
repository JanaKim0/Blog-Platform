import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { toHttpParams } from './http-params';
import { Comment, CommentRequest, Page } from './models';

@Injectable({ providedIn: 'root' })
export class CommentService {
  private readonly http = inject(HttpClient);

  list(slug: string, page = 0, size = 20): Observable<Page<Comment>> {
    return this.http.get<Page<Comment>>(`/api/articles/${encodeURIComponent(slug)}/comments`, {
      params: toHttpParams({ page, size }),
    });
  }

  add(slug: string, request: CommentRequest): Observable<Comment> {
    return this.http.post<Comment>(
      `/api/articles/${encodeURIComponent(slug)}/comments`,
      request,
    );
  }

  /** A comment is addressed by its own id once it exists. */
  update(id: number, request: CommentRequest): Observable<Comment> {
    return this.http.put<Comment>(`/api/comments/${id}`, request);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`/api/comments/${id}`);
  }
}
