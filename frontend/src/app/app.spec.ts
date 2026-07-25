import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';
import { authInterceptor } from './core/auth-interceptor';

describe('App', () => {
  beforeEach(async () => {
    // The service reads the stored token when it is constructed, so the
    // browser storage has to start clean for each test.
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        // The interceptor is part of what is being tested here: it is what puts
        // the bearer token on the request.
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
  });

  it('creates the shell', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('shows the credit line and links it to the repository', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const root = fixture.nativeElement as HTMLElement;
    expect(root.textContent).toContain('Built by Jana Kim · 2026');

    const link = root.querySelector<HTMLAnchorElement>('.site-footer a');
    expect(link?.textContent?.trim()).toBe('Source code');
    expect(link?.href).toContain('github.com/JanaKim0/Blog-Platform');
  });

  it('offers signing in when nobody is signed in', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const root = fixture.nativeElement as HTMLElement;
    expect(root.textContent).toContain('Sign in');
    expect(root.textContent).not.toContain('Sign out');
  });

  it('does not ask the server who the user is when there is no token', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    TestBed.inject(HttpTestingController).expectNone('/api/auth/me');
  });

  it('confirms a stored token with the server on startup', async () => {
    localStorage.setItem('blog.token', 'a-stored-token');

    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const http = TestBed.inject(HttpTestingController);
    const probe = http.expectOne('/api/auth/me');
    expect(probe.request.headers.get('Authorization')).toBe('Bearer a-stored-token');
    probe.flush({
      id: 1,
      username: 'nora',
      email: 'nora@example.com',
      displayName: 'Nora',
      bio: null,
      avatarUrl: null,
      role: 'USER',
      createdAt: new Date().toISOString(),
    });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Sign out');
  });
});
