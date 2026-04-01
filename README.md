# Libri API

Backend service for the Libri book library ecosystem. Manages the book catalogue,
serves cover images, orchestrates the Go crawler, and exposes a REST API for the
Vue admin panel and React Native mobile app.

## Tech stack

- Kotlin + Spring Boot 4
- PostgreSQL via Supabase
- Flyway for schema migrations
- JWT authentication via Supabase Auth (ES256)

## Prerequisites

- Java 21
- A [Supabase](https://supabase.com) project with email auth enabled
- [libri-crawler](https://github.com/masiama/libri-crawler) built binary

## Configuration

### Gradle properties

Create `~/.gradle/gradle.properties` with your GitHub credentials for pushing Docker images:

```properties
ghcr.username=your-github-username
ghcr.token=your-github-token
```

The token needs `write:packages` scope.

## Getting started

**1. Clone and configure**

```bash
cp .env.example .env
```

Fill in `.env`

**2. Create the images directory**

```bash
mkdir -p $IMAGES_DIR  # or whatever path you set in .env
```

**3. Run**

```bash
./gradlew bootRun
```

## Authentication

All endpoints except `/api/v1/ping` require a valid Supabase JWT passed as:

```
Authorization: Bearer <token>
```

Admin endpoints additionally require the `is_admin: true` claim in `app_metadata`.
Set this on a user via the Supabase service role API:

```bash
curl -X PUT "https://your-project-ref.supabase.co/auth/v1/admin/users/<user-uuid>" \
  -H "apikey: <supabase-publishable-key>" \
  -H "Authorization: Bearer <supabase-secret-key>" \
  -H "Content-Type: application/json" \
  -d '{"app_metadata": {"is_admin": true}}'
```

## API

### Public

| Method | Endpoint       | Description  |
|--------|----------------|--------------|
| GET    | `/api/v1/ping` | Health check |

### Authenticated

| Method | Endpoint                    | Description            |
|--------|-----------------------------|------------------------|
| GET    | `/api/v1/books`             | List books (paginated) |
| GET    | `/api/v1/books/{isbn}`      | Get book by ISBN       |
| GET    | `/api/v1/images/{isbn}.jpg` | Get cover image        |

### Admin only

| Method | Endpoint                       | Description                           |
|--------|--------------------------------|---------------------------------------|
| POST   | `/api/v1/admin/crawl`          | Trigger crawl for all enabled sources |
| POST   | `/api/v1/admin/crawl/{source}` | Trigger crawl for a specific source   |
| GET    | `/api/v1/admin/crawl/status`   | Get last 10 crawl job statuses        |

### Internal (crawler only)

Protected by `X-Internal-Key` header, not JWT.

| Method | Endpoint                        | Description                |
|--------|---------------------------------|----------------------------|
| POST   | `/api/v1/internal/books/batch`  | Upsert a batch of books    |
| GET    | `/api/v1/internal/books/exists` | Check if a book URL exists |

## Database migrations

Managed by Flyway. Migrations run automatically on startup from
`src/main/resources/db/migration/`.

## Architecture

Libri API is the central service in the Libri ecosystem:

- **[libri-crawler](https://github.com/masiama/libri-crawler)** scrapes book metadata and sends it here via the internal
  API
- Cover images are stored on local disk in a shared directory
