# ADR 0003: Integration Testing with Testcontainers and PostgreSQL

## Status
Accepted

## Context
Using in-memory databases (like H2) for Spring Boot integration testing often leads to subtle bugs due to SQL syntax discrepancies, different dialect features (JSONB, Flyway migration compatibility), and unexpected behavior differences compared to production PostgreSQL.

## Decision
We mandated **Testcontainers** for all integration tests (`*IT.java`):

1. **Production Parity:** Tests launch a real PostgreSQL 16 Docker container during Maven test execution (`mvnw verify -Pintegration-tests`).
2. **Flyway Migration Verification:** Flyway database migrations run directly against the Testcontainers PostgreSQL instance during test suite setup.
3. **Isolated Test Execution:** Each test execution runs against a clean, isolated database state.

## Consequences

### Positive
- 100% SQL compatibility parity between local tests and production AWS RDS PostgreSQL.
- Detects database migration failures early in the CI/CD pipeline.

### Negative
- Requires Docker environment to execute integration tests locally and in CI.
- Increases integration test execution time slightly compared to in-memory databases.
