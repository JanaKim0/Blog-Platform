import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ArticleSummary } from '../../core/models';
import { Avatar } from '../avatar/avatar';

/** One article in a list. Presentational only - it fetches nothing itself. */
@Component({
  selector: 'app-article-card',
  imports: [RouterLink, DatePipe, Avatar],
  templateUrl: './article-card.html',
  styleUrl: './article-card.scss',
})
export class ArticleCard {
  readonly article = input.required<ArticleSummary>();
  /** Shows the DRAFT marker; only used on the author's own list. */
  readonly showStatus = input(false);
}
