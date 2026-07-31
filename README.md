# NoteFlow — IT4409 Personal Note App

NoteFlow is a bilingual, server-rendered personal note manager built for the
IT4409 final project. Users can securely create, view, edit, delete, pin,
search, and categorize their own notes from a responsive desktop or mobile
interface.

Vietnamese is the default language. English, light mode, dark mode, and system
theme detection are included.

## Features

- Account registration and session-based form login
- BCrypt password hashing
- Strict owner-scoped note access
- Complete note CRUD with detail pages
- Personal, Work, Study, Idea, and Other categories
- Category filtering and debounced live search
- Stable pagination, infinite scrolling, and Load More fallback
- Pin and unpin notes
- Vietnamese and English localization
- Light, dark, and system themes
- Responsive Bootstrap interface
- Server-side Jakarta Bean Validation
- Centralized 400, 404, and 500 error handling
- Idempotent instructor demo account
- PostgreSQL persistence
- Focused security and validation tests

## Technology stack

- Java 21
- Spring Boot 4.1
- Spring MVC and Thymeleaf
- Spring Security
- Spring Data JPA and Hibernate
- Jakarta Bean Validation
- PostgreSQL
- Bootstrap 5 and Bootstrap Icons
- Vanilla JavaScript
- Maven
- Docker and Docker Compose

## Architecture

```text
Browser
  |
  | HTML pages, forms, and Thymeleaf fragment requests
  v
Spring MVC controllers
  |
  v
Service layer (business rules and ownership enforcement)
  |
  v
Spring Data JPA repositories
  |
  v
PostgreSQL
```

The application is a single Spring Boot monolith. Authentication uses
server-side HTTP sessions; it does not use JWT. Every note query combines the
note or filter criteria with the authenticated owner's database ID. A missing
note and another user's note therefore produce the same `404 Not Found`
response.

## Prerequisites

- JDK 21 configured in the IDE
- Docker Desktop with Docker Compose
- An IDE such as IntelliJ IDEA

No Node.js or frontend build pipeline is required.

## Local development

### 1. Start PostgreSQL

From the project directory:

```bash
docker compose -f compose.dev.yaml up -d
```

The development database is available only on `127.0.0.1:5432`. Its default
local credentials match `application-dev.yaml`:

```text
Database: note_app
Username: note_user
Password: note_password
```

These credentials are for local development only.

### 2. Run Spring Boot from the IDE

Run:

```text
ziang.com.it4409.noteapp.NoteAppApplication
```

The `dev` profile is the default profile. Open:

```text
http://localhost:8080
```

Default local demo account:

```text
Username: demo
Password: demo12345
```

The demo initializer creates the account only when it does not already exist.
It never resets the password on restart.

### 3. Stop the development database

```bash
docker compose -f compose.dev.yaml down
```

The named volume keeps PostgreSQL data. To intentionally delete all local
application data, remove the `note-app-dev_note_app_dev_data` volume manually.

## Run as a local JAR

Keep the development PostgreSQL container running, then build:

```powershell
.\mvnw.cmd clean package
java -jar target\NoteApp-0.0.1-SNAPSHOT.jar
```

On macOS or Linux:

```bash
./mvnw clean package
java -jar target/NoteApp-0.0.1-SNAPSHOT.jar
```

## Tests

```powershell
.\mvnw.cmd test
```

The focused test suite covers:

- Duplicate username and email rejection
- BCrypt password storage
- View, edit, and delete ownership isolation
- Owner-scoped search and category filtering
- Note validation
- Authenticated-owner assignment during creation
- Idempotent demo-account initialization
- Full Spring application context startup with H2

## Production with Docker Compose

### 1. Configure secrets

```bash
cp .env.example .env
```

Set strong, unique values for `DB_PASSWORD` and `DEMO_PASSWORD`. Do not commit
`.env`.

### 2. Build and start

```bash
docker compose up -d --build
```

Production runs two containers:

- `note-app`: Spring Boot on container port 8080
- `postgres`: PostgreSQL with a persistent named volume

Only the application is bound to the host, at `127.0.0.1:8080`. PostgreSQL has
no public host port.

Useful commands:

```bash
docker compose ps
docker compose logs -f note-app
docker compose down
```

Do not run `docker compose down -v` unless you intentionally want to delete the
production database.

## VPS, Tailscale, and Cloudflare Tunnel

The application is prepared to receive forwarded headers and is intended to
remain bound to:

```text
http://127.0.0.1:8080
```

The existing Cloudflare Tunnel should forward the public hostname to that
address. Tailscale can be used independently for private VPS administration;
the database does not need to be exposed through Tailscale or the public
internet.

If the final VPS deployment uses a systemd-managed JAR instead of the
application container, use the `prod` profile and supply the same environment
variables listed in `.env.example`.

## Configuration

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Use `prod` outside local development |
| `DB_HOST` | PostgreSQL host; `postgres` inside Compose |
| `DB_PORT` | PostgreSQL port, normally `5432` |
| `DB_NAME` | Database name |
| `DB_USERNAME` | Database login |
| `DB_PASSWORD` | Database password |
| `DEMO_USERNAME` | Instructor demo username |
| `DEMO_EMAIL` | Instructor demo email |
| `DEMO_PASSWORD` | Initial instructor demo password |

Hibernate uses `ddl-auto: update` as specified by the project plan. PostgreSQL
data is persisted in a named Docker volume.

## Interface behavior

Search waits 300 ms after typing and cancels stale requests. Search and category
filters are combined on the server and always include the current user's ID.
Results are returned as localized Thymeleaf HTML fragments. Infinite scrolling
and the Load More button use the same paginated endpoint and stable order:
pinned first, then most recently updated, then highest ID.

The theme choice is stored under `note-app-theme` in browser `localStorage`.
Language choice is stored in a cookie. User-created titles and note content are
never translated.

## Screenshots

Add final screenshots before submission:

- `<LANDING_PAGE_SCREENSHOT>`
- `<NOTES_DESKTOP_SCREENSHOT>`
- `<NOTES_MOBILE_SCREENSHOT>`
- `<DARK_MODE_SCREENSHOT>`

## Final submission information

```text
Student ID: <STUDENT_ID>
Student name: <STUDENT_NAME>
Student email: <STUDENT_EMAIL>
Source repository: <REPOSITORY_URL>
Public demo: <DEMO_URL>
Demo username: <DEMO_USERNAME>
Demo password: <DEMO_PASSWORD>
```

The course submission outline is kept outside this repository.
