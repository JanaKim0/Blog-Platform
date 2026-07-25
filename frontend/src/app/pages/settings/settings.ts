import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { errorMessage, fieldErrors } from '../../core/api-error';
import { AuthService } from '../../core/auth-service';
import { CurrentUser } from '../../core/models';
import { UserService } from '../../core/user-service';
import { Avatar } from '../../shared/avatar/avatar';

/** One's own account: the profile, the picture and the password. */
@Component({
  selector: 'app-settings',
  imports: [ReactiveFormsModule, RouterLink, Avatar],
  templateUrl: './settings.html',
  styleUrl: './settings.scss',
})
export class Settings {
  private readonly users = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly auth = inject(AuthService);

  protected readonly savingProfile = signal(false);
  protected readonly profileSaved = signal(false);
  protected readonly profileError = signal<string | null>(null);
  protected readonly profileFieldErrors = signal<Record<string, string>>({});

  protected readonly avatarBusy = signal(false);
  protected readonly avatarError = signal<string | null>(null);
  protected readonly avatarPreview = signal<string | null>(null);

  protected readonly changingPassword = signal(false);
  protected readonly passwordChanged = signal(false);
  protected readonly passwordError = signal<string | null>(null);

  protected readonly profileForm = this.formBuilder.nonNullable.group({
    displayName: ['', Validators.maxLength(100)],
    bio: ['', Validators.maxLength(1000)],
    email: ['', [Validators.required, Validators.email]],
  });

  protected readonly passwordForm = this.formBuilder.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  constructor() {
    const user = this.auth.currentUser();
    if (user) {
      this.fill(user);
    }
    this.destroyRef.onDestroy(() => this.releasePreview());
  }

  private fill(user: CurrentUser): void {
    this.profileForm.setValue({
      displayName: user.displayName ?? '',
      bio: user.bio ?? '',
      email: user.email,
    });
  }

  protected saveProfile(): void {
    if (this.profileForm.invalid || this.savingProfile()) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.savingProfile.set(true);
    this.profileError.set(null);
    this.profileSaved.set(false);
    this.profileFieldErrors.set({});

    const raw = this.profileForm.getRawValue();
    this.users
      .updateProfile({
        displayName: raw.displayName.trim() === '' ? null : raw.displayName.trim(),
        bio: raw.bio.trim() === '' ? null : raw.bio.trim(),
        email: raw.email.trim(),
      })
      .subscribe({
        next: (user) => {
          // The header shows the name and picture, so it has to hear about this.
          this.auth.setCurrentUser(user);
          this.savingProfile.set(false);
          this.profileSaved.set(true);
        },
        error: (failure: unknown) => {
          this.savingProfile.set(false);
          this.profileError.set(errorMessage(failure, 'Could not save your profile'));
          this.profileFieldErrors.set(fieldErrors(failure));
        },
      });
  }

  protected onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.releasePreview();
    this.avatarPreview.set(URL.createObjectURL(file));
    this.avatarBusy.set(true);
    this.avatarError.set(null);

    this.users.uploadAvatar(file).subscribe({
      next: (user) => {
        this.auth.setCurrentUser(user);
        this.avatarBusy.set(false);
        // The stored picture is now the real one; the local preview can go.
        this.releasePreview();
        input.value = '';
      },
      error: (failure: unknown) => {
        this.avatarBusy.set(false);
        this.releasePreview();
        input.value = '';
        this.avatarError.set(errorMessage(failure, 'Could not upload the picture'));
      },
    });
  }

  protected removeAvatar(): void {
    if (this.avatarBusy() || !this.auth.currentUser()?.avatarUrl) {
      return;
    }
    this.avatarBusy.set(true);
    this.users.removeAvatar().subscribe({
      next: (user) => {
        this.auth.setCurrentUser(user);
        this.avatarBusy.set(false);
      },
      error: (failure: unknown) => {
        this.avatarBusy.set(false);
        this.avatarError.set(errorMessage(failure, 'Could not remove the picture'));
      },
    });
  }

  protected changePassword(): void {
    if (this.passwordForm.invalid || this.changingPassword()) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.changingPassword.set(true);
    this.passwordError.set(null);
    this.passwordChanged.set(false);

    this.users.changePassword(this.passwordForm.getRawValue()).subscribe({
      next: () => {
        this.changingPassword.set(false);
        this.passwordChanged.set(true);
        this.passwordForm.reset({ currentPassword: '', newPassword: '' });
      },
      error: (failure: unknown) => {
        this.changingPassword.set(false);
        this.passwordError.set(errorMessage(failure, 'Could not change your password'));
      },
    });
  }

  private releasePreview(): void {
    const preview = this.avatarPreview();
    if (preview) {
      URL.revokeObjectURL(preview);
      this.avatarPreview.set(null);
    }
  }
}
