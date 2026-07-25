import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { errorMessage, fieldErrors } from '../../core/api-error';
import { AuthService } from '../../core/auth-service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  /** Per-field messages the server sent back, e.g. "Username is already taken". */
  protected readonly serverFieldErrors = signal<Record<string, string>>({});

  // The rules mirror the backend's validation, so the obvious mistakes are
  // caught before a request is made - but the server stays the authority.
  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: [
      '',
      [Validators.required, Validators.minLength(3), Validators.maxLength(50), Validators.pattern(/^[A-Za-z0-9._-]+$/)],
    ],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    displayName: ['', Validators.maxLength(100)],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    this.serverFieldErrors.set({});

    const { username, email, password, displayName } = this.form.getRawValue();
    this.auth
      .register({
        username,
        email,
        password,
        displayName: displayName.trim() === '' ? undefined : displayName.trim(),
      })
      .subscribe({
        next: () => this.router.navigate(['/']),
        error: (failure: unknown) => {
          this.submitting.set(false);
          this.error.set(errorMessage(failure, 'Could not create the account'));
          this.serverFieldErrors.set(fieldErrors(failure));
        },
      });
  }
}
