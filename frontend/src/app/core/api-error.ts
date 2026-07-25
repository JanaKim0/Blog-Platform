import { HttpErrorResponse } from '@angular/common/http';

import { ApiError } from './models';

/**
 * The backend answers every failure with the same {@link ApiError} shape, which
 * is what lets these two helpers exist instead of each form picking the message
 * apart itself.
 */

export function errorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Cannot reach the server. Is the backend running?';
    }
    const body = error.error as ApiError | null;
    if (body?.message) {
      return body.message;
    }
  }
  return fallback;
}

/** Per-field validation messages, keyed by field name. Empty when there are none. */
export function fieldErrors(error: unknown): Record<string, string> {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as ApiError | null;
    if (body?.fieldErrors) {
      return body.fieldErrors;
    }
  }
  return {};
}
