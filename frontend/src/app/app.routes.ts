import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './core/guards';

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
    path: 'feed',
    title: 'My feed · Blog Platform',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/feed/feed').then((m) => m.Feed),
  },
  {
    path: 'people',
    title: 'People · Blog Platform',
    loadComponent: () => import('./pages/people/people').then((m) => m.People),
  },
  {
    path: 'settings',
    title: 'Your account · Blog Platform',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/settings/settings').then((m) => m.Settings),
  },
  {
    // The two connection lists share one component; the route says which it is.
    path: 'authors/:username/followers',
    title: 'Followers · Blog Platform',
    data: { mode: 'followers' },
    loadComponent: () => import('./pages/connections/connections').then((m) => m.Connections),
  },
  {
    path: 'authors/:username/following',
    title: 'Following · Blog Platform',
    data: { mode: 'following' },
    loadComponent: () => import('./pages/connections/connections').then((m) => m.Connections),
  },
  {
    path: 'authors/:username',
    title: 'Author · Blog Platform',
    loadComponent: () => import('./pages/profile/profile').then((m) => m.Profile),
  },
  {
    path: 'write',
    title: 'Write an article · Blog Platform',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/editor/editor').then((m) => m.Editor),
  },
  {
    path: 'my-articles',
    title: 'My articles · Blog Platform',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/my-articles/my-articles').then((m) => m.MyArticles),
  },
  {
    // Listed before the article route for readability; the router would not
    // confuse them anyway, since a two-segment path cannot match three.
    path: 'articles/:slug/edit',
    title: 'Edit article · Blog Platform',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/editor/editor').then((m) => m.Editor),
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
