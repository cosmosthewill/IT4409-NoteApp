# IT4409 Final Project Implementation Plan

## 1. Project Overview

Build a production-ready bilingual web application for personal note management.

The application must satisfy the IT4409 final project requirements:

- Full CRUD for the main data entity.
- Every note must belong to a user.
- Each user must only see and modify their own notes.
- At least one classification/filtering field.
- Responsive desktop and mobile interface.
- Input validation.
- Centralized error handling.
- Appropriate HTTP status codes.
- A publicly accessible deployed demo.
- A predefined demo account for the instructor.

The application will be a server-rendered Spring Boot monolith. Do not use React, Vue, JWT, microservices, or a separate frontend application.

## 2. Final Technology Stack

### Backend and server-rendered frontend

- Java 21
- Spring Boot
- Spring MVC
- Thymeleaf
- Spring Security
- Spring Data JPA
- Jakarta Bean Validation
- Maven

### User interface

- Bootstrap 5
- Thymeleaf fragments
- Vanilla JavaScript
- Bootstrap Icons, if useful
- No frontend build pipeline is required

### Database and deployment

- PostgreSQL
- Docker
- Docker Compose
- One Spring Boot application container
- One PostgreSQL container
- Persistent named Docker volume
- Cloudflare Tunnel already running on the VPS
- Cloudflare Tunnel forwards traffic to `http://localhost:8080`

## 3. Architecture

Use a conventional layered monolithic architecture:

```text
Browser
  |
  | HTTP requests, HTML forms, fragment requests
  v
Spring MVC Controllers
  |
  v
Service Layer
  |
  v
Spring Data JPA Repositories
  |
  v
PostgreSQL
```

Thymeleaf renders complete HTML pages and reusable HTML fragments.

Vanilla JavaScript is used only for:

- Live search
- Infinite scrolling
- Load-more fallback
- Dark-mode selection
- Delete confirmation
- Small user-interface interactions

Keep business rules and security enforcement on the server.

## 4. Recommended Project Structure

```text
note-app/
├── src/
│   ├── main/
│   │   ├── java/com/example/noteapp/
│   │   │   ├── NoteAppApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── LocaleConfig.java
│   │   │   │   └── DemoDataInitializer.java
│   │   │   ├── auth/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── RegistrationService.java
│   │   │   │   └── dto/
│   │   │   │       └── RegistrationForm.java
│   │   │   ├── user/
│   │   │   │   ├── User.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── CustomUserDetailsService.java
│   │   │   ├── note/
│   │   │   │   ├── Note.java
│   │   │   │   ├── NoteCategory.java
│   │   │   │   ├── NoteController.java
│   │   │   │   ├── NoteService.java
│   │   │   │   ├── NoteRepository.java
│   │   │   │   └── dto/
│   │   │   │       └── NoteForm.java
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── NoteNotFoundException.java
│   │   │       └── DuplicateUserException.java
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── fragments/
│   │       │   │   ├── head.html
│   │       │   │   ├── navbar.html
│   │       │   │   ├── flash-messages.html
│   │       │   │   └── footer.html
│   │       │   ├── landing.html
│   │       │   ├── auth/
│   │       │   │   ├── login.html
│   │       │   │   └── register.html
│   │       │   ├── notes/
│   │       │   │   ├── list.html
│   │       │   │   ├── detail.html
│   │       │   │   ├── form.html
│   │       │   │   └── fragments/
│   │       │   │       └── note-cards.html
│   │       │   └── error/
│   │       │       ├── 400.html
│   │       │       ├── 404.html
│   │       │       └── 500.html
│   │       ├── static/
│   │       │   ├── css/app.css
│   │       │   └── js/
│   │       │       ├── theme.js
│   │       │       ├── notes.js
│   │       │       └── delete-confirmation.js
│   │       ├── messages.properties
│   │       ├── messages_vi.properties
│   │       ├── messages_en.properties
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/
├── Dockerfile
├── compose.yaml
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

Small structural adjustments are acceptable, but keep clear separation among controllers, services, repositories, entities, form objects, configuration, and exception handling.

## 5. Domain Model

### 5.1 User

Fields:

- `id`: `Long`, generated primary key
- `username`: required, unique, 3-50 characters
- `email`: required, unique, valid email address
- `passwordHash`: required
- `createdAt`
- `updatedAt`

Only one role is needed: normal user.

An admin panel is out of scope.

### 5.2 Note

Fields:

- `id`: `Long`, generated primary key
- `title`: required, maximum 150 characters
- `content`: required, maximum 10,000 characters
- `category`: required enum
- `pinned`: boolean, default `false`
- `user`: required many-to-one owner
- `createdAt`
- `updatedAt`

Use automatic timestamps with JPA auditing or entity lifecycle callbacks.

### 5.3 Note categories

Use a fixed enum:

```java
PERSONAL,
WORK,
STUDY,
IDEA,
OTHER
```

Store enum codes in the database with `EnumType.STRING`.

Do not store translated category labels in the database.

## 6. Database Strategy

Use PostgreSQL and Spring Data JPA.

Do not add Flyway or Liquibase for this project.

Use Hibernate to create and update the schema directly:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

This project is deployed directly as a small academic production application, so automatic schema update is acceptable.

Use a persistent named Docker volume for PostgreSQL data.

Never run PostgreSQL and Spring Boot in the same container.

Recommended indexes:

- Unique index on `users.username`
- Unique index on `users.email`
- Index on `notes.user_id`
- Composite index on `notes.user_id, notes.category`
- Composite index supporting owner-based ordering by pin and update time

Avoid exposing the PostgreSQL port publicly in production.

## 7. Authentication and Authorization

Use Spring Security form login with server-side HTTP sessions.

Do not use JWT.

Password requirements:

- Minimum 8 characters
- Store only BCrypt hashes
- Never store plain-text passwords

Public routes:

- `/`
- `/login`
- `/register`
- `/css/**`
- `/js/**`
- `/images/**`
- Other required static assets

Authenticated routes:

- `/notes`
- `/notes/**`

Behavior:

- An unauthenticated visitor opening `/` sees the landing page.
- The landing page contains Login and Register actions.
- An authenticated user opening `/` is redirected to `/notes`.
- Successful login redirects directly to `/notes`.
- Successful registration redirects to `/login?registered`.
- Successful logout redirects to `/?logout`.

Keep CSRF protection enabled.

All state-changing actions must use POST requests with CSRF tokens.

Do not implement note deletion with a GET request.

## 8. Ownership Security

Ownership enforcement is mandatory.

Never accept `userId` from a browser form or query parameter to decide note ownership.

The authenticated user must be obtained from Spring Security.

When creating a note:

1. Resolve the current authenticated user.
2. Assign that user as the note owner.
3. Save the note.

When reading, editing, pinning, or deleting a note, query using both note ID and current user ID.

Example repository method:

```java
Optional<Note> findByIdAndUserId(Long noteId, Long userId);
```

List and search queries must always include the current user ID.

If a note does not exist or belongs to another user, return the same `404 Not Found` result. Do not reveal whether another user's note exists.

## 9. Default Demo Account

Create one predefined demo account at application startup.

Use an idempotent initializer:

- If the configured demo username does not exist, create the user.
- If it already exists, do nothing.
- Do not overwrite its password on every restart.

Read credentials from environment variables:

```text
DEMO_USERNAME
DEMO_PASSWORD
DEMO_EMAIL
```

Suggested defaults for local development may be documented in `.env.example`, but production credentials must not be hard-coded in Java source.

Hash the demo password with BCrypt before saving it.

The application must also allow normal users to register from the landing page.

## 10. Main Page Flow

### 10.1 Landing page

Route:

```text
GET /
```

For unauthenticated visitors, display:

- Application name
- Short description
- Login button
- Register button
- Language switcher
- Theme switcher
- Responsive mobile and desktop layout

For authenticated visitors:

```text
Redirect to /notes
```

### 10.2 Registration

Routes:

```text
GET  /register
POST /register
```

Fields:

- Username
- Email
- Password
- Confirm password

Validation:

- Username required and unique
- Email required, valid, and unique
- Password minimum 8 characters
- Confirmation must match password

### 10.3 Login

Use Spring Security form login.

Fields:

- Username
- Password

Show localized messages for:

- Invalid credentials
- Successful registration
- Successful logout

### 10.4 Notes

Routes:

```text
GET  /notes
GET  /notes/new
POST /notes
GET  /notes/{id}
GET  /notes/{id}/edit
POST /notes/{id}
POST /notes/{id}/delete
POST /notes/{id}/pin
```

A PUT or DELETE method override is optional. Standard HTML POST forms are acceptable and simpler.

## 11. Note CRUD Behavior

### Create

- Validate the form.
- Assign current user as owner.
- Save.
- Add localized success flash message.
- Redirect to `/notes`.

### Read list

- Display only the current user's notes.
- Default page size: 12.
- Default ordering:
  1. `pinned DESC`
  2. `updatedAt DESC`
  3. `id DESC`

### Read detail

- Verify ownership.
- Display full title, content, category, pin status, created time, and updated time.

### Update

- Verify ownership.
- Validate input.
- Never allow owner reassignment.
- Save and redirect with a success message.

### Delete

- Verify ownership.
- Show a Bootstrap confirmation modal before submitting.
- Delete permanently.
- Redirect with a success message.

No trash or recycle-bin feature is required.

### Pin and unpin

- Verify ownership.
- Toggle the pinned state.
- Return to the notes list or previous page.
- Pinned notes appear before unpinned notes.

## 12. Filtering and Search

### Category filter

Allow filtering by:

- All
- Personal
- Work
- Study
- Idea
- Other

Filtering must work together with search and infinite scrolling.

### Live search

Search title and content.

Requirements:

- Vanilla JavaScript
- 300 ms debounce
- Trim input
- Empty search restores the complete list
- Use `AbortController` to cancel stale requests
- Reset pagination to page 0 whenever the keyword or category changes
- Show loading state
- Show localized empty state
- Show localized error state
- Search only the authenticated user's notes

Recommended fragment endpoint:

```text
GET /notes/fragments?q={keyword}&category={category}&page={page}
```

The endpoint returns a Thymeleaf HTML fragment, not JSON.

## 13. Infinite Scroll

Use Spring Data pagination.

Default:

- 12 notes per page
- Stable ordering by pin, updated time, and ID

Frontend behavior:

- Render page 0 in the original `/notes` response.
- Place a sentinel element below the note grid.
- Use `IntersectionObserver`.
- When the sentinel becomes visible, request the next page.
- Append returned note cards.
- Maintain `isLoading`, `currentPage`, and `hasNext` state.
- Never request the same page twice.
- Preserve the current search keyword and category.
- Stop observing when there is no next page.

Also provide a visible Load More button as a fallback.

The button and infinite scroll must use the same loading function.

When keyword or category changes:

1. Cancel the previous request.
2. Reset `currentPage` to 0.
3. Clear the current note grid.
4. Load and replace page 0.
5. Resume infinite scrolling if another page exists.

## 14. Responsive User Interface

Use Bootstrap 5.

Target layouts:

- Mobile: one note card per row
- Tablet: two note cards per row
- Desktop: three note cards per row

The notes list should include:

- Navbar
- Application name
- Current username
- Language selector
- Theme selector
- Logout action
- Search field
- Category filter
- New Note button
- Responsive note-card grid
- Empty state
- Loading indicator
- Infinite-scroll sentinel
- Load More fallback

Each note card should show:

- Pin indicator
- Title
- Truncated plain-text content preview
- Localized category badge
- Last updated time
- View action
- Edit action
- Delete action

Avoid excessive custom CSS. Use Bootstrap utility classes where practical.

## 15. Dark Mode

Support:

- Light
- Dark
- System

Use Bootstrap's `data-bs-theme` attribute.

Store the selected mode in browser `localStorage`.

Suggested key:

```text
note-app-theme
```

Apply the selected theme as early as possible in the page head to prevent a light-theme flash before dark mode is applied.

Theme preference does not need to be stored in PostgreSQL.

All pages, forms, cards, modals, validation errors, and error pages must remain readable in both themes.

## 16. Internationalization

Support two languages:

- Vietnamese (`vi`)
- English (`en`)

Default language:

```text
Vietnamese
```

Use Spring message bundles:

```text
messages.properties
messages_vi.properties
messages_en.properties
```

Store locale selection in a cookie.

Include language switching on:

- Landing page
- Login page
- Registration page
- Notes pages
- Error pages

Translate:

- Navigation
- Buttons
- Labels
- Placeholders
- Authentication messages
- Validation messages
- Flash messages
- Delete confirmation
- Loading state
- Empty state
- Error state
- Category display names
- Error pages
- Theme labels

Do not translate user-created note titles or content.

Store category enum values as stable English codes and localize only their displayed labels.

Live-search and infinite-scroll fragment responses must use the active locale.

## 17. Validation

Use dedicated form DTOs instead of binding browser input directly to JPA entities.

### RegistrationForm

Validate:

- Username: required, 3-50 characters
- Email: required, valid format
- Password: required, minimum 8 characters
- Confirm password: must match

### NoteForm

Validate:

- Title: required, maximum 150 characters
- Content: required, maximum 10,000 characters
- Category: required

Show field-specific validation errors in Thymeleaf templates.

Frontend validation may improve usability, but backend validation remains authoritative.

## 18. Error Handling and HTTP Behavior

Use centralized MVC exception handling with `@ControllerAdvice`.

Required cases:

- Invalid form data: render form with errors, HTTP 400 where appropriate
- Unauthenticated access: Spring Security redirects to login
- Note missing or not owned by current user: HTTP 404
- Duplicate username or email: HTTP 409 or a validated registration form response
- Unexpected error: HTTP 500

Provide localized custom pages:

- 400
- 404
- 500

Do not display stack traces or sensitive internal details to users.

Use Post/Redirect/Get after successful form submissions.

## 19. Application Configuration

Use profiles:

```text
dev
prod
```

### Development profile

- Spring Boot may run directly from the IDE.
- PostgreSQL runs in Docker.
- Thymeleaf cache disabled.
- Database URL points to localhost.

### Production profile

- Spring Boot runs inside its container.
- PostgreSQL hostname is the Compose service name.
- Thymeleaf cache enabled.
- Secrets come from environment variables.
- Application binds to port 8080.
- Configure forwarded-header support for Cloudflare Tunnel.

Recommended property:

```yaml
server:
  forward-headers-strategy: framework
```

Database schema strategy:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

## 20. Docker Deployment

Use two containers in one Compose project:

```text
note-app
postgres
```

### Application container

- Multi-stage Docker build
- Build the Maven project
- Copy the generated executable JAR into a Java 21 JRE image
- Run as a non-root user where practical
- Expose container port 8080
- Bind host port to `127.0.0.1:8080`

### PostgreSQL container

- Official PostgreSQL image
- Persistent named volume
- Health check
- No public host port in production

### Compose requirements

- App depends on a healthy PostgreSQL service
- Both services use `restart: unless-stopped`
- Secrets are read from `.env`
- Commit `.env.example`
- Do not commit `.env`

Suggested deployment command:

```bash
docker compose up -d --build
```

Cloudflare Tunnel continues to target:

```text
http://localhost:8080
```

Stopping containers without deleting data:

```bash
docker compose down
```

Do not use `docker compose down -v` unless intentionally deleting the database.

## 21. Dockerfile Direction

Use a multi-stage Dockerfile:

1. Maven and Java 21 builder stage
2. Java 21 JRE runtime stage
3. Copy the built JAR
4. Run `java -jar /app/app.jar`

The final VPS should not require Maven or a host-installed Java runtime.

## 22. Environment Variables

Document at least:

```text
SPRING_PROFILES_ACTIVE=prod
DB_HOST=postgres
DB_PORT=5432
DB_NAME=note_app
DB_USERNAME=note_user
DB_PASSWORD=change_me
DEMO_USERNAME=demo
DEMO_EMAIL=demo@example.com
DEMO_PASSWORD=change_me
```

Create `.env.example` with safe placeholders.

## 23. Out-of-Scope Features

Do not implement:

- React
- Vue
- Angular
- JWT
- REST-only architecture
- Microservices
- Admin dashboard
- Social login
- Email verification
- Forgot-password flow
- File attachments
- Rich-text editor
- Markdown editor
- Note sharing
- Collaboration
- Custom category CRUD
- Trash or recycle bin
- WebSocket
- Redis
- Kubernetes

The goal is a polished, secure, understandable academic Note App, not an oversized platform.

## 24. Testing Expectations

Add focused tests for important backend behavior.

At minimum:

- Registration rejects duplicate username.
- Registration rejects duplicate email.
- Password is stored as a BCrypt hash.
- User A cannot view User B's note.
- User A cannot edit User B's note.
- User A cannot delete User B's note.
- Search returns only the current user's notes.
- Category filtering returns only the current user's matching notes.
- Note validation rejects blank title/content.
- Demo account initialization is idempotent.

Full browser end-to-end automation is optional.

## 25. README Requirements

The README must include:

- Project description
- Feature list
- Technology stack
- Architecture summary
- Prerequisites
- Local development instructions
- Docker production instructions
- Environment-variable setup
- Demo account setup
- Cloudflare Tunnel deployment note
- Default URLs
- Screenshots placeholders
- Repository link placeholder
- Public demo link placeholder

Also include a concise explanation of:

- Session-based authentication
- BCrypt password hashing
- Per-user note ownership
- PostgreSQL persistence
- Live search
- Infinite scrolling
- Internationalization
- Dark mode

## 26. Final Submission Information Placeholders

Keep placeholders for information required in the final course PDF:

```text
Student ID: <STUDENT_ID>
Student name: <STUDENT_NAME>
Student email: <STUDENT_EMAIL>
Source repository: <REPOSITORY_URL>
Public demo: <DEMO_URL>
Demo username: <DEMO_USERNAME>
Demo password: <DEMO_PASSWORD>
```

The final project documentation must include:

- Student information
- Feature description
- Run instructions
- Simple architecture diagram
- Technologies used
- Source-code link
- Working deployment link
- Demo credentials

## 27. Implementation Order

Implement in this order:

1. Create Spring Boot project and dependencies.
2. Configure PostgreSQL and profiles.
3. Implement User entity and repository.
4. Configure Spring Security and BCrypt.
5. Implement registration and login pages.
6. Add demo account initializer.
7. Implement Note entity, enum, repository, service, and forms.
8. Implement secure owner-scoped CRUD.
9. Implement Thymeleaf pages and Bootstrap responsive layout.
10. Add category filtering and stable pagination.
11. Add Thymeleaf note-card fragment endpoint.
12. Add live search.
13. Add infinite scrolling and Load More fallback.
14. Add pin/unpin.
15. Add dark mode.
16. Add Vietnamese and English localization.
17. Add centralized exception handling and error pages.
18. Add focused tests.
19. Add Dockerfile and Compose configuration.
20. Deploy through the existing Cloudflare Tunnel.
21. Complete README and final submission documentation.

## 28. Definition of Done

The project is complete when:

- A visitor can open the landing page.
- A visitor can register or use the seeded demo account.
- Login redirects directly to the notes page.
- A user can create, view, edit, delete, pin, search, and filter notes.
- Live search works without a full page reload.
- Infinite scrolling loads additional owner-scoped results correctly.
- Load More works as a fallback.
- Every note operation is restricted to its owner.
- Vietnamese and English both work across full pages and fragments.
- Light, dark, and system themes work.
- The interface is responsive on mobile and desktop.
- Validation messages are clear and localized.
- Error handling is centralized.
- The application and PostgreSQL run as two Docker containers.
- PostgreSQL data survives normal container recreation.
- The application is reachable through the existing Cloudflare Tunnel.
- The public demo and demo account work for the instructor.
