import { Component, computed, input, output } from '@angular/core';

/**
 * Previous/next paging with a page count. Deliberately not a numbered pager:
 * a blog feed is browsed, not indexed, and a long row of page numbers is mostly
 * noise on a phone.
 */
@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.html',
  styleUrl: './pagination.scss',
})
export class Pagination {
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly first = input.required<boolean>();
  readonly last = input.required<boolean>();

  readonly pageChange = output<number>();

  /** One page of results needs no controls at all. */
  protected readonly visible = computed(() => this.totalPages() > 1);
  protected readonly humanPage = computed(() => this.page() + 1);

  protected previous(): void {
    if (!this.first()) {
      this.pageChange.emit(this.page() - 1);
    }
  }

  protected next(): void {
    if (!this.last()) {
      this.pageChange.emit(this.page() + 1);
    }
  }
}
