/**
 * The shapes the API returns. These mirror the backend DTOs one to one, so a
 * change on the server shows up here as a compile error rather than as
 * `undefined` in a template.
 */

export type ArticleStatus = 'DRAFT' | 'PUBLISHED';
export type ArticleSort = 'RECENT' | 'LIKES' | 'COMMENTS';
export type Role = 'USER' | 'ADMIN';

/** Every paginated endpoint answers with this. */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface UserSummary {
  id: number;
  username: string;
  displayName: string;
  avatarUrl: string | null;
}

/** The signed-in user's own account, including the private fields. */
export interface CurrentUser {
  id: number;
  username: string;
  email: string;
  displayName: string;
  bio: string | null;
  avatarUrl: string | null;
  role: Role;
  createdAt: string;
}

/** Somebody else's public profile - never carries an email address. */
export interface Profile {
  id: number;
  username: string;
  displayName: string;
  bio: string | null;
  avatarUrl: string | null;
  joinedAt: string;
  articlesCount: number;
  followersCount: number;
  followingCount: number;
  following: boolean;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: CurrentUser;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string | null;
}

export interface Tag {
  id: number;
  name: string;
  slug: string;
}

/** A feed card: everything about an article except its body. */
export interface ArticleSummary {
  id: number;
  slug: string;
  title: string;
  summary: string | null;
  coverUrl: string | null;
  author: UserSummary;
  category: Category | null;
  tags: Tag[];
  status: ArticleStatus;
  publishedAt: string | null;
  updatedAt: string;
  likesCount: number;
  commentsCount: number;
  likedByMe: boolean;
}

export interface Article extends ArticleSummary {
  content: string;
  createdAt: string;
}

export interface Comment {
  id: number;
  content: string;
  author: UserSummary;
  createdAt: string;
  updatedAt: string;
  edited: boolean;
}

/** The single error shape the API uses for every failure. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  fieldErrors?: Record<string, string>;
}

// ---- Request payloads ----

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  displayName?: string;
}

export interface LoginRequest {
  login: string;
  password: string;
}

export interface UpdateProfileRequest {
  displayName: string | null;
  bio: string | null;
  email: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ArticleRequest {
  title: string;
  summary: string | null;
  content: string;
  categorySlug: string | null;
  tags: string[];
  status: ArticleStatus;
}

export interface CommentRequest {
  content: string;
}

/** Filters for the article list; omitted fields are simply not applied. */
export interface ArticleQuery {
  query?: string;
  category?: string;
  tag?: string;
  author?: string;
  sortBy?: ArticleSort;
  page?: number;
  size?: number;
}
