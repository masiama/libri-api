# Libri API

Kotlin/Spring Boot backend for the Libri catalog.

It stores book metadata in PostgreSQL, serves cover images from local storage,
exposes authenticated endpoints for the admin UI, and manages a purgatory queue
for books with invalid ISBNs.

The system processes crawl jobs asynchronously via Redis: the API enqueues crawl
commands, and a separate crawler service consumes them and publishes typed crawl
events back through Redis.

These events are processed in real time and streamed to the UI via SSE.

## Stack

- Kotlin
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Redis
- Flyway
- Spring Security resource server

## Requirements

- Java 21
- PostgreSQL
- Redis
- A Clerk application with a JWKS endpoint

## Redis integration

The system uses Redis as the only communication layer between API and crawler.

### Command queue

```
crawler:commands
```

Used to enqueue crawl jobs from the API.

### Event queue

```
crawl:events
```

Used by the crawler to publish:

- scraped books
- progress updates
- completion events
- error events

Events are consumed asynchronously by the API and streamed to clients.

---

## Configuration

The application reads environment variables through Spring config and local
`.env` loading in development.

Required variables:

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=libri
DB_USER=libri
DB_PASS=secret

REDIS_HOST=localhost
REDIS_PORT=6373

CLERK_JWKS_URL=https://example.clerk.accounts.dev/.well-known/jwks.json
IMAGES_DIR=/path/to/images
```

Create the image directory before startup:

```bash
mkdir -p "$IMAGES_DIR"
```

---

## Development

### Running locally

```bash
make
```

This starts the following processes in parallel:

- `bootRun` — runs the Spring Boot application
- `build --continuous` — recompiles on file changes, triggering devtools hot
  reload

---

## Authentication and authorization

All endpoints except `/api/v1/ping` and `/api/v1/images/**` require a valid
Bearer token.

```
Authorization: Bearer <token>
```

Admin endpoints require the authenticated user to have `is_admin: true`, which
is mapped to `ROLE_ADMIN`.

Set this in Clerk dashboard → Users → select user → Public metadata:

```json
{
  "is_admin": true
}
```

---

## API surface

### Public

- `GET /api/v1/ping`
- `GET /api/v1/images/{isbn}.jpg`

### Authenticated

- `GET /api/v1/books`
- `GET /api/v1/books/{code}`
- `GET /api/v1/sources`

### Admin

- `POST /api/v1/admin/books`
- `PUT /api/v1/admin/books/{isbn}`
- `DELETE /api/v1/admin/books/{isbn}`
- `POST /api/v1/admin/crawl`
- `POST /api/v1/admin/crawl/{source}`
- `GET /api/v1/admin/crawl`
- `GET /api/v1/admin/crawl/events`
- `GET /api/v1/admin/purgatory`
- `POST /api/v1/admin/purgatory/{id}/approve`
- `DELETE /api/v1/admin/purgatory/{id}`

---

## Book upload format

Book create and update requests use `multipart/form-data`:

- `book` — JSON payload with book metadata
- `file` — uploaded cover image

On `POST /api/v1/admin/books`, `file` is required. On
`PUT /api/v1/admin/books/{isbn}`, `file` is optional.

---

## Crawl system behavior

- API enqueues crawl jobs into Redis (`crawler:commands`)
- crawler consumes jobs using a blocking queue (`BLPOP`)
- crawler publishes events into Redis (`crawl:events`)
- API consumes events and:
  - updates database state
  - buffers and batch-inserts books
  - streams updates to frontend via SSE

### Concurrency model

- Only one crawl per source can run at a time
- Duplicate crawl requests are rejected by the crawler using Redis locks
- Locks have TTL and are periodically extended by the crawler

---

## Database

Schema changes are managed with Flyway migrations in:

```
src/main/resources/db/migration/
```

Migrations run automatically on startup.
