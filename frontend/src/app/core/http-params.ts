import { HttpParams } from '@angular/common/http';

/**
 * Builds a query string from a plain object, leaving out anything empty.
 *
 * The API treats a missing parameter as "do not filter by this", so sending
 * `category=` would not be the same as not sending it at all.
 */
export function toHttpParams(values: Record<string, string | number | undefined | null>): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(values)) {
    if (value === undefined || value === null) {
      continue;
    }
    const asString = String(value).trim();
    if (asString !== '') {
      params = params.set(key, asString);
    }
  }
  return params;
}
