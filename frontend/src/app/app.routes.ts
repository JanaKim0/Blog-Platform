import { Routes } from '@angular/router';

import { guestGuard } from './core/guards';

/**
 * Every page is loaded lazily, so the first paint does not carry code for pages
 * the visitor may never open.
 */
export const routes: Routes = [
  {
    path: '',
    title: 'Blog Platform',
    loadComponent: () => import('./pages/home/home').then((m) => m.Home),
  },
  {
    // The title is replaced with the article's own once it loads.
    path: 'articles/:slug',
    title: 'Article · Blog Platform',
    loadComponent: () => import('./pages/article/article').then((m) => m.Article),
  },
  {
    path: 'login',
    title: 'Sign in · Blog Platform',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    title: 'Create an account · Blog Platform',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/register/register').then((m) => m.Register),
  },
  {
    path: '**',
    title: 'Not found · Blog Platform',
    loadComponent: () => import('./pages/not-found/not-found').then((m) => m.NotFound),
  },
];
