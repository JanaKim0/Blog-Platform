import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth-service';

/**
 * Protects the pages that need an account. The attempted URL is carried along
 * as `returnUrl`, so signing in takes the user where they were going instead of
 * dumping them on the home page.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isSignedIn()) {
    return true;
  }
  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};

/** Keeps a signed-in user off the sign-in and registration pages. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isSignedIn()) {
    return true;
  }
  router.navigate(['/']);
  return false;
};
