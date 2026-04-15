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

## Running locally

Start the app:

```bash
./gradlew bootRun
```

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
- `GET /api/v1/images/{isbn}.{ext}`

### Authenticated

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

## Docker publishing

The Gradle build includes Jib configuration for publishing an image to GHCR.

If you use that flow, add credentials to `~/.gradle/gradle.properties`:

```properties
ghcr.username=your-github-username
ghcr.token=your-github-token
```

The token needs `write:packages` scope.
