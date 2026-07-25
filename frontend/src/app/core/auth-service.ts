import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { AuthResponse, CurrentUser, LoginRequest, RegisterRequest } from './models';

const TOKEN_KEY = 'blog.token';

/**
 * Holds the session. The token lives in localStorage so a page reload does not
 * sign the user out; the user object is re-fetched from `/api/auth/me` on
 * startup rather than cached, so a profile edited in another tab is not shown
 * stale here.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly currentUserSignal = signal<CurrentUser | null>(null);
  private readonly tokenSignal = signal<string | null>(localStorage.getItem(TOKEN_KEY));

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isSignedIn = computed(() => this.tokenSignal() !== null);
  readonly isAdmin = computed(() => this.currentUserSignal()?.role === 'ADMIN');

  get token(): string | null {
    return this.tokenSignal();
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', request)
      .pipe(tap((response) => this.startSession(response)));
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', request)
      .pipe(tap((response) => this.startSession(response)));
  }

  /**
   * Called once when the app starts. A stored token that the server rejects
   * simply ends the session instead of leaving the UI in a half-signed-in state.
   */
  restoreSession(): Observable<CurrentUser> | null {
    if (!this.tokenSignal()) {
      return null;
    }
    return this.http
      .get<CurrentUser>('/api/auth/me')
      .pipe(tap((user) => this.currentUserSignal.set(user)));
  }

  /** Keeps the header in step after the user edits their own profile. */
  setCurrentUser(user: CurrentUser): void {
    this.currentUserSignal.set(user);
  }

  signOut(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.tokenSignal.set(null);
    this.currentUserSignal.set(null);
  }

  private startSession(response: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, response.token);
    this.tokenSignal.set(response.token);
    this.currentUserSignal.set(response.user);
  }
}
