# Blog Platform

A blogging platform built with **Angular** and **Spring Boot**.

Users can write their own articles, read other people's blogs, follow authors,
comment and leave likes. Articles are organised with categories and tags, can be
searched and filtered, and are served through a paginated feed.

> This project is being built stage by stage — see the commit history.

## Features

- [ ] Registration and login (stateless JWT)
- [ ] User profile and profile editing
- [ ] Creating, editing and deleting articles
- [ ] Image upload (article covers, avatars)
- [ ] Comments
- [ ] Likes
- [ ] Following authors
- [ ] Article search
- [ ] User search
- [ ] Categories and tags
- [ ] Sorting and filtering
- [ ] Feed of the latest publications

## Getting Started

### Prerequisites

- **Java 17+**
- **Node.js 20+** and **npm**
- **PostgreSQL 14+** — the primary database
  *(or run without it using the embedded H2 profile, see below)*

### 1. Database

Create the database and a development role once, as the `postgres` superuser:

```bash
psql -U postgres -h localhost -c "CREATE ROLE blog LOGIN PASSWORD 'blog';" -c "CREATE DATABASE blogplatform OWNER blog;"
```

The backend reads its credentials from `DB_URL` / `DB_USER` / `DB_PASSWORD`
environment variables and falls back to `blog` / `blog` for local development.

### 2. Backend — Spring Boot API (port `8080`)

```bash
cd backend
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`. The Maven Wrapper downloads Maven
automatically, so no local Maven installation is required.

To run without PostgreSQL, start the embedded H2 profile instead:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

### 3. Frontend — Angular app (port `4200`)

```bash
cd frontend
npm install
npm start
```

Then open <http://localhost:4200>.

## Tech Stack

| Layer     | Technology                                        |
|-----------|---------------------------------------------------|
| Frontend  | Angular 22, TypeScript, SCSS, RxJS                |
| Backend   | Spring Boot 4, Spring Web MVC, Spring Security    |
| Data      | Spring Data JPA / Hibernate, PostgreSQL 17, H2    |
| Auth      | JWT (JJWT), BCrypt password hashing               |
| Build     | Maven (wrapper), npm / Angular CLI                |

## Architecture

```
Blog Platform/
├── backend/          Spring Boot REST API
│   └── src/main/java/com/blogplatform/
└── frontend/         Angular single-page application
    └── src/app/
```

The backend is a stateless REST API: the browser authenticates once and then
sends a JWT in the `Authorization` header with every request. The Angular app is
a separate single-page application that talks to the API over HTTP.

The database schema and the reasoning behind it are documented in
[docs/DATABASE.md](docs/DATABASE.md).

A detailed description of the layers and the API endpoints is added as the
project grows.
