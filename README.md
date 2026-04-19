# Libri API

Kotlin/Spring Boot backend for the Libri catalog.

It stores book metadata in PostgreSQL, serves cover images from local storage,
exposes authenticated endpoints for the admin UI, and provides internal endpoints
used by the crawler.

## Stack

- Kotlin
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Security resource server

## Requirements

- Java 21
- PostgreSQL
- A Clerk application with a JWKS endpoint
- A built [`libri-crawler`](https://github.com/masiama/libri-crawler) binary if you
  want to trigger crawls from the API

## Configuration

The application reads environment variables through Spring config and local `.env`
loading in development.

Required variables:

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=libri
DB_USER=libri
DB_PASS=secret
CLERK_JWKS_URL=https://example.clerk.accounts.dev/.well-known/jwks.json
IMAGES_DIR=/absolute/path/to/images
CRAWLER_BINARY_PATH=/absolute/path/to/libri-crawler/bin/crawler
INTERNAL_API_KEY=change-me
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

Create the image directory before startup:

```bash
mkdir -p "$IMAGES_DIR"
```

## Development

### Running locally

```bash
make
```

This starts the following processes in parallel:

* `bootRun` — runs the Spring Boot application
* `build --continuous` — recompiles on file changes, triggering devtools hot reload

If you also have a local clone of [libri-crawler](https://github.com/masiama/libri-crawler),
you can enable automatic rebuilding of the crawler binary on file changes.

### Optional: Crawler integration

To enable crawler watching:

1. Install `watchexec`:

```bash
brew install watchexec
```

2. Set `DEV_CRAWLER_DIR` in your `.env` to point to your local crawler repo:

```bash
DEV_CRAWLER_DIR=../libri-crawler
```

When configured, an additional process will run:

* Crawler watcher — rebuilds the Go binary on file changes

If `DEV_CRAWLER_DIR` is not set, the crawler watcher is skipped automatically and only the API runs.

## Authentication and authorization

All endpoints except `/api/v1/ping`, `/api/v1/images/**`, and `/api/v1/internal/**`
require a valid Bearer token.

```
Authorization: Bearer <token>
```

Admin endpoints require the authenticated user to have `is_admin: true`, which is
mapped to `ROLE_ADMIN`.

Set this in Clerk dashboard → Users → select user → Public metadata:

```json
{
  "is_admin": true
}
```

Internal crawler endpoints use `X-Internal-Key` instead of JWT authentication.

## API surface

### Public

- `GET /api/v1/ping`
- `GET /api/v1/images/{isbn}.jpg`

### Authenticated

- `POST /api/v1/books`
- `GET /api/v1/books`
- `GET /api/v1/books/{isbn}`
- `GET /api/v1/sources`
- `PUT /api/v1/books/{isbn}`

### Admin

- `POST /api/v1/admin/crawl`
- `POST /api/v1/admin/crawl/{source}`
- `GET /api/v1/admin/crawl/status`

### Internal (crawler only)

Protected by `X-Internal-Key`, not JWT.

- `POST /api/v1/internal/books/batch`
- `GET /api/v1/internal/books/exists`

## Database

Schema changes are managed with Flyway migrations in:

```text
src/main/resources/db/migration/
```

Migrations run automatically on startup.
