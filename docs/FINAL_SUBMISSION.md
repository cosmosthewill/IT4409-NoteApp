# IT4409 Final Submission Draft

Use this document as the source for:

```text
[IT4409]_CuoiKy20252_MaSoHocVien_HoTenHocVien.pdf
```

## Student information

- Student ID: `<STUDENT_ID>`
- Full name: `<STUDENT_NAME>`
- Email: `<STUDENT_EMAIL>`

## Project

- Product: NoteFlow personal note manager
- Source repository: `<REPOSITORY_URL>`
- Public demo: `<DEMO_URL>`
- Demo username: `<DEMO_USERNAME>`
- Demo password: `<DEMO_PASSWORD>`

## Functional description

NoteFlow allows each registered user to create, list, view, edit, delete, pin,
search, and categorize personal notes. Every note belongs to exactly one user.
All detail and mutation operations query by both note ID and authenticated user
ID, so users cannot discover or modify another user's data.

Categories:

- Personal
- Work
- Study
- Idea
- Other

Additional interface features include Vietnamese and English localization,
light/dark/system themes, responsive note cards, live search, infinite
scrolling, and a Load More fallback.

## Architecture

```text
Web browser
    |
    v
Spring MVC + Thymeleaf controllers
    |
    v
Service layer
    |
    v
Spring Data JPA repositories
    |
    v
PostgreSQL
```

The deployment contains one Spring Boot application and one PostgreSQL
database. Cloudflare Tunnel exposes only the application on localhost port
8080. PostgreSQL remains private.

## Technologies

- Java 21 and Spring Boot 4.1
- Spring MVC, Thymeleaf, and Spring Security
- Spring Data JPA, Hibernate, and PostgreSQL
- Jakarta Bean Validation
- Bootstrap 5, Bootstrap Icons, and Vanilla JavaScript
- Maven, Docker, and Docker Compose

## Run instructions

### Local development

1. Start PostgreSQL:
   `docker compose -f compose.dev.yaml up -d`
2. Run `NoteAppApplication` from the IDE.
3. Open `http://localhost:8080`.

### Production

1. Copy `.env.example` to `.env`.
2. Replace all placeholder passwords.
3. Run `docker compose up -d --build`.
4. Configure Cloudflare Tunnel to forward to `http://localhost:8080`.

## Validation evidence to add

- Screenshot of the responsive landing page
- Screenshot of a populated notes list
- Screenshot of note creation validation
- Screenshot of mobile layout
- Screenshot of English or dark-mode interface
- Screenshot or terminal capture of passing tests
- Final repository URL
- Final public demo URL
- Confirmed instructor demo credentials
