# AGENTS.md

## Project

Vehicle Maintenance History is a full-stack vehicle maintenance management
platform.

Main components:

- Spring Boot backend
- React web application
- PostgreSQL database
- Kotlin Multiplatform mobile application
- AWS infrastructure for production

## Development Principles

Before making changes:

1. Read the task specification.
2. Inspect the existing implementation.
3. Inspect related tests.
4. Understand the existing architecture.
5. Make the smallest change that completely satisfies the task.

Do not implement unrelated improvements.

## Architecture

Backend:

- Java 21
- Spring Boot 3.5
- Spring Security
- Spring Data
- PostgreSQL
- Flyway
- JWT authentication
- OpenAPI

Frontend:

- React
- TypeScript
- Vite

Mobile:

- Kotlin Multiplatform
- Compose Multiplatform
- Ktor
- Room KMP
- Offline-first architecture
- Outbox Pattern

## Database

All schema changes must use Flyway migrations.

Never modify an already-applied migration.

Create a new migration instead.

## API

API behavior must remain backward compatible unless the task explicitly
requires a breaking change.

When changing an API:

- Update OpenAPI documentation.
- Update tests.
- Update related clients when required.

Use consistent HTTP status codes and error responses.

## Authentication

Authentication uses JWT and refresh tokens.

Never:

- commit secrets
- log tokens
- expose credentials
- hardcode production credentials

Authentication changes must include appropriate tests.

## Testing

Behavior changes must include tests.

Prefer:

- unit tests for isolated logic
- integration tests for API/database behavior
- Testcontainers for database integration

Before completing a task, run the relevant test suite.

Do not remove or weaken existing tests just to make a task pass.

## Configuration

Use environment variables or configuration files for environment-specific
values.

Local development must work without AWS.

AWS infrastructure must not be required for normal local development.

Use Docker Compose for local PostgreSQL and other required local services.

## AWS

AWS is production infrastructure.

Do not introduce AWS dependencies into local development unless explicitly
required.

Do not enable or deploy paid AWS infrastructure as part of normal development.

Storage should be abstracted so local development can use local storage while
production can use S3.

## Dependencies

Do not add a dependency unless it provides clear value for the current task.

Prefer existing project dependencies and established project patterns.

## Code Quality

Follow the existing project style.

Prefer:

- clear names
- small classes/methods
- explicit responsibilities
- dependency inversion
- SOLID principles
- maintainable code over clever abstractions

Avoid premature abstractions.

## Roadmap

Tasks use the VMH identifier format:

VMH-XXX.YY

Examples:

- VMH-004.01
- VMH-004.02
- VMH-005.01

The task ID must appear in:

- task documentation
- GitHub Issues
- pull requests
- commit messages when appropriate

## Scope Control

Do not:

- refactor unrelated code
- rename unrelated classes
- change formatting across unrelated files
- upgrade dependencies without a task
- introduce new architecture without a requirement

If an improvement is discovered during implementation, document it as a
separate task instead of silently expanding the current scope.

## Git

Keep commits focused.

Prefer one logical change per commit.

Do not commit:

- secrets
- credentials
- generated local files
- IDE-specific files
- production configuration containing sensitive values

## Completion Checklist

Before considering a task complete:

- [ ] Requirements implemented
- [ ] Tests added or updated
- [ ] Existing tests passing
- [ ] API documentation updated when necessary
- [ ] Database migration created when necessary
- [ ] No unrelated changes
- [ ] No secrets committed
- [ ] Local development still works