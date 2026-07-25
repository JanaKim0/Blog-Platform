import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { UserSummary } from '../../core/models';
import { Avatar } from '../avatar/avatar';

/** One person in a list: search results, followers, following. */
@Component({
  selector: 'app-user-card',
  imports: [RouterLink, Avatar],
  templateUrl: './user-card.html',
  styleUrl: './user-card.scss',
})
export class UserCard {
  readonly user = input.required<UserSummary>();
}
