import { Component } from '@angular/core';

/** Shown on every page, so the credit line can never drift between them. */
@Component({
  selector: 'app-footer',
  templateUrl: './footer.html',
  styleUrl: './footer.scss',
})
export class Footer {
  protected readonly repositoryUrl = 'https://github.com/JanaKim0/Blog-Platform';
}
