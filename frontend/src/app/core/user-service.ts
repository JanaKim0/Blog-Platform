import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { toHttpParams } from './http-params';
import {
  ChangePasswordRequest,
  CurrentUser,
  Page,
  Profile,
  UpdateProfileRequest,
  UserSummary,
} from './models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  /** An empty query lists everyone, which is what an open search page shows. */
  search(query: string, page = 0, size = 20): Observable<Page<UserSummary>> {
    return this.http.get<Page<UserSummary>>('/api/users', {
      params: toHttpParams({ query, page, size }),
    });
  }

  profile(username: string): Observable<Profile> {
    return this.http.get<Profile>(`/api/users/${encodeURIComponent(username)}`);
  }

  updateProfile(request: UpdateProfileRequest): Observable<CurrentUser> {
    return this.http.put<CurrentUser>('/api/users/me', request);
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.put<void>('/api/users/me/password', request);
  }

  uploadAvatar(file: File): Observable<CurrentUser> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<CurrentUser>('/api/users/me/avatar', form);
  }

  removeAvatar(): Observable<CurrentUser> {
    return this.http.delete<CurrentUser>('/api/users/me/avatar');
  }

  /** Both answer with the profile, so the new counts come back with it. */
  follow(username: string): Observable<Profile> {
    return this.http.post<Profile>(`/api/users/${encodeURIComponent(username)}/follow`, {});
  }

  unfollow(username: string): Observable<Profile> {
    return this.http.delete<Profile>(`/api/users/${encodeURIComponent(username)}/follow`);
  }

  followers(username: string, page = 0, size = 20): Observable<Page<UserSummary>> {
    return this.http.get<Page<UserSummary>>(
      `/api/users/${encodeURIComponent(username)}/followers`,
      { params: toHttpParams({ page, size }) },
    );
  }

  following(username: string, page = 0, size = 20): Observable<Page<UserSummary>> {
    return this.http.get<Page<UserSummary>>(
      `/api/users/${encodeURIComponent(username)}/following`,
      { params: toHttpParams({ page, size }) },
    );
  }
}
