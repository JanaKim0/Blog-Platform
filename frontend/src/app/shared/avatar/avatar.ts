import { Component, computed, input } from '@angular/core';

/**
 * A user's picture, or the first letter of their name when they have none.
 * Used by the header, feed cards, comments and profiles, so the fallback looks
 * the same everywhere.
 */
@Component({
  selector: 'app-avatar',
  templateUrl: './avatar.html',
  styleUrl: './avatar.scss',
  host: { '[class]': 'sizeClass()' },
})
export class Avatar {
  readonly url = input<string | null>(null);
  readonly name = input<string>('');
  readonly size = input<'sm' | 'md' | 'lg'>('md');

  protected readonly sizeClass = computed(() => `size-${this.size()}`);
  protected readonly initial = computed(() => (this.name().trim() || '?').charAt(0).toUpperCase());
}
