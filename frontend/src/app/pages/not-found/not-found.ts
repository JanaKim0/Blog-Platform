import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  template: `
    <div class="container page">
      <div class="empty">
        <h1>Nothing here</h1>
        <p>This page does not exist, or it was removed.</p>
        <a class="btn btn-secondary" routerLink="/">Back to the latest articles</a>
      </div>
    </div>
  `,
})
export class NotFound {}
