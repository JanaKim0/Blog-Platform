import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth-service';
import { Footer } from './layout/footer/footer';
import { Header } from './layout/header/header';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Header, Footer],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  private readonly auth = inject(AuthService);

  ngOnInit(): void {
    // A token in localStorage says who the user claims to be; only the server
    // can confirm it, so the app asks once on startup. A rejected token ends the
    // session quietly - see the interceptor.
    this.auth.restoreSession()?.subscribe({ error: () => undefined });
  }
}
