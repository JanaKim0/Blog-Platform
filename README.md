# Blog Platform

A blogging platform built with **Angular** and **Spring Boot**.

Users write their own articles, read other people's blogs, follow authors,
comment and leave likes. Articles are organised with categories and tags, can be
searched and filtered, and are served through a paginated feed.

## Features

- ✔ Registration and login (stateless JWT)
- ✔ User profile and profile editing
- ✔ Creating, editing and deleting articles
- ✔ Drafts and publishing
- ✔ Image upload — article covers and avatars
- ✔ Comments
- ✔ Likes
- ✔ Following authors
- ✔ A personal feed of the authors you follow
- ✔ Article search
- ✔ User search
- ✔ Categories and tags
- ✔ Sorting by date, likes or comments
- ✔ Feed of the latest publications, paginated

## Getting Started

### Prerequisites

- **Java 17+**
- **Node.js 20+** and **npm**
- **PostgreSQL 14+** — the primary database
  *(or run without it on the embedded H2 profile, see below)*

Maven is **not** required: the project ships with the Maven Wrapper.

### 1. Database

Create the database and a development role once, as the `postgres` superuser:

```bash
psql -U postgres -h localhost -c "CREATE ROLE blog LOGIN PASSWORD 'blog';" -c "CREATE DATABASE blogplatform OWNER blog;"
```

On Windows, `psql` usually lives at
`C:\Program Files\PostgreSQL\17\bin\psql.exe`.

The backend reads its connection details from the `DB_URL`, `DB_USER` and
`DB_PASSWORD` environment variables and falls back to `blog` / `blog` for local
development. The schema itself is created by Hibernate on first start.

### 2. Backend — Spring Boot API on port `8080`

```bash
cd backend
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`.

To start **without PostgreSQL**, on an embedded H2 database:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

To fill an **empty** database with demo content — three authors, six articles,
comments, likes and subscriptions:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.demo-data=true
```

The demo accounts are `nora`, `mira` and `lena`, all with the password
`demo-password`. The seeder never touches a database that already has accounts.

### 3. Frontend — Angular app on port `4200`

```bash
cd frontend
npm install
npm start
```

Then open <http://localhost:4200>.

The dev server proxies `/api` and `/uploads` to `http://localhost:8080`
(`proxy.conf.json`), so the browser talks to a single origin and the image URLs
the API returns work as they are.

### Running the tests

```bash
cd backend && ./mvnw test
```

```bash
cd frontend && npm test
```

126 backend tests and 11 frontend tests.

## Tech Stack

| Layer     | Technology                                             |
|-----------|--------------------------------------------------------|
| Frontend  | Angular 22, TypeScript, SCSS, RxJS, signals            |
| Backend   | Spring Boot 4, Spring Web MVC, Spring Security         |
| Data      | Spring Data JPA / Hibernate, PostgreSQL 17, H2         |
| Auth      | JWT (JJWT), BCrypt password hashing                    |
| Images    | `ImageIO` — validation, re-encoding and downscaling    |
| Build     | Maven (wrapper), npm / Angular CLI                     |
| Testing   | JUnit 5, MockMvc, `@DataJpaTest`, Vitest               |

## Architecture

```
Blog Platform/
├── backend/                        Spring Boot REST API
│   └── src/main/java/com/blogplatform/
│       ├── domain/                 JPA entities and enums
│       ├── repository/             Spring Data repositories
│       ├── service/                business rules
│       ├── web/                    REST controllers
│       ├── dto/                    request and response records
│       ├── security/               JWT issuing, filter, user details
│       ├── config/                 security, CORS, storage, seeding
│       ├── exception/              error types and the global handler
│       └── util/                   slug generation
├── frontend/                       Angular single-page application
│   └── src/app/
│       ├── core/                   services, models, interceptor, guards
│       ├── layout/                 header and footer
│       ├── pages/                  one folder per route
│       └── shared/                 avatar, pager, cards
└── docs/DATABASE.md                schema and the reasoning behind it
```

The backend is a **stateless** REST API: the browser signs in once and then
sends a JWT in the `Authorization` header with every request. There is no
session and no CSRF token. The Angular app is a separate single-page
application that talks to it over HTTP.

### Decisions worth explaining

**Slugs never change.** An article's address is derived from its first title and
then left alone, because every link to the article uses it. Cyrillic is
transliterated rather than dropped, so a Russian title gets a readable address
instead of `article-2`.

**Drafts answer 404, not 403.** A 403 would confirm that the article exists,
which is exactly what a draft should not do. Its author gets it normally.

**The database enforces what matters.** Unique constraints on
`(article, user)` for likes and `(follower, author)` for subscriptions mean a
double like or a double follow is impossible even if two requests arrive at the
same moment. The services treat the resulting violation as success, because the
end state is the one that was asked for.

**No denormalised counters.** Likes and comments are counted with queries, so
they cannot drift out of sync. A whole page of articles is assembled with three
queries rather than two per card.

**Listing queries select ids, then load them.** Paginating a query that also
fetches a collection forces Hibernate to read every matching row and apply the
page in memory; splitting it in two keeps the pagination in SQL.

**Uploads are decoded and re-encoded, never copied through.** That rejects files
that only claim to be images, scales oversized photos down, and drops metadata
such as GPS coordinates on the way.

**Only the author of a comment may edit it** — not the article's author, not an
administrator. Moderation is deletion; rewriting somebody else's words is not
something authority should allow.

**Feed state lives in the URL.** A filtered, sorted, paginated list can be
bookmarked or sent to somebody, and the back button behaves as expected.

**Article bodies are rendered as text, never as HTML**, with line breaks kept by
CSS.

The database schema and the thinking behind it are documented in
[docs/DATABASE.md](docs/DATABASE.md).

## API

| Method | Endpoint | Auth | |
|---|---|---|---|
| POST | `/api/auth/register` | — | create an account |
| POST | `/api/auth/login` | — | sign in with username **or** email |
| GET | `/api/auth/me` | ✔ | the current account |
| GET | `/api/articles` | — | feed: `query`, `category`, `tag`, `author`, `sortBy`, `page`, `size` |
| POST | `/api/articles` | ✔ | write an article |
| GET | `/api/articles/{slug}` | — | one article (drafts: author only) |
| PUT | `/api/articles/{slug}` | ✔ | edit — author or admin |
| DELETE | `/api/articles/{slug}` | ✔ | delete — author or admin |
| POST/DELETE | `/api/articles/{slug}/cover` | ✔ | cover image |
| POST/DELETE | `/api/articles/{slug}/like` | ✔ | like / unlike |
| GET/POST | `/api/articles/{slug}/comments` | —/✔ | read / add comments |
| PUT | `/api/comments/{id}` | ✔ | edit — comment author only |
| DELETE | `/api/comments/{id}` | ✔ | delete — comment author, article author or admin |
| GET | `/api/me/articles` | ✔ | own articles, drafts included |
| GET | `/api/me/feed` | ✔ | articles by followed authors |
| GET | `/api/users` | — | search people |
| GET | `/api/users/{username}` | — | public profile |
| PUT | `/api/users/me` | ✔ | edit own profile |
| PUT | `/api/users/me/password` | ✔ | change password |
| POST/DELETE | `/api/users/me/avatar` | ✔ | avatar |
| POST/DELETE | `/api/users/{username}/follow` | ✔ | follow / unfollow |
| GET | `/api/users/{username}/followers` | — | followers |
| GET | `/api/users/{username}/following` | — | followed authors |
| GET | `/api/categories` | — | category list (admin may add and remove) |
| GET | `/api/tags` | — | every tag in use |

Every failure comes back in the same shape — `timestamp`, `status`, `error`,
`message` and, for validation errors, `fieldErrors` — so the frontend has one
thing to parse.

## Credits

Built by **Jana Kim**.

Developed together with **Claude** (Anthropic) as a pair-programming partner:
architecture and design decisions were discussed and reviewed jointly, and the
implementation was written stage by stage — each stage a single commit with its
own tests.
