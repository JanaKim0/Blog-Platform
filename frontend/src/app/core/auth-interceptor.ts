import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth-service';

/**
 * Attaches the bearer token to every API call and ends the session when the
 * server answers 401 - that means the token expired or was revoked, and the app
 * should stop pretending the user is signed in.
 *
 * Three cases, deliberately handled differently:
 *
 * - sign-in and registration get no token at all, and a 401 from them means a
 *   wrong password, not an expired session
 * - `/api/auth/me` is the session probe run at startup. An expired token there
 *   ends the session quietly; redirecting would throw a reader off a public page
 *   they were entitled to see
 * - everything else redirects to the sign-in page, remembering where the user was
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const isCredentialsCall =
    request.url.includes('/api/auth/login') || request.url.includes('/api/auth/register');
  const isSessionProbe = request.url.includes('/api/auth/me');

  const token = auth.token;
  const outgoing =
    token && !isCredentialsCall
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;

  return next(outgoing).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isCredentialsCall) {
        auth.signOut();
        if (!isSessionProbe) {
          router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
        }
      }
      return throwError(() => error);
    }),
  );
};
