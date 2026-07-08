# Vehicle Maintenance History

REST API to manage vehicles and their maintenance history.

## Stack

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- JWT
- Docker
- Kubernetes/Helm deployment manifests for a public AWS demo environment
- GitHub Actions CI/CD for PR validation and AWS demo deployment
- Testcontainers
- Swagger/OpenAPI
- Spring Actuator

## Features

- User registration and login
- JWT authentication
- Refresh token and logout
- Vehicle CRUD
- Maintenance CRUD by vehicle
- Standard error responses
- Database migrations with Flyway
- Unit and integration tests
- Basic observability with `X-Request-Id`

## Running locally with Docker

Start the full stack:

```bash
docker compose up --build
```

Frontend:

```text
http://localhost:5173
```

API:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Health:

```text
http://localhost:8080/actuator/health
```

Stop the stack:

```bash
docker compose down
```

Start only the database:

```bash
docker compose up postgres
```

## Running tests

Unit and controller tests:

```bash
.\mvnw.cmd test
```

Full backend validation, including integration tests:

```bash
.\mvnw.cmd verify -Pintegration-tests
```

Integration tests use Testcontainers, so Docker must be running.

## Authentication

Protected endpoints require:

```http
Authorization: Bearer <accessToken>
```

Basic flow:

```text
1. Register user
2. Receive accessToken and refreshToken
3. Use accessToken on protected endpoints
4. Use refreshToken to renew tokens
```

## Main endpoints

### Auth

| Method | Endpoint | Description |
|---|---|---|
| POST | `/v1/auth/register` | Register user |
| POST | `/v1/auth/login` | Login |
| POST | `/v1/auth/refresh` | Refresh tokens |
| POST | `/v1/auth/logout` | Logout |

### Vehicles

| Method | Endpoint | Description |
|---|---|---|
| POST | `/v1/vehicles` | Create vehicle |
| GET | `/v1/vehicles` | List user vehicles |
| GET | `/v1/vehicles/{vehicleId}` | Find vehicle |
| PUT | `/v1/vehicles/{vehicleId}` | Update vehicle |
| DELETE | `/v1/vehicles/{vehicleId}` | Delete vehicle |

### Maintenances

| Method | Endpoint | Description |
|---|---|---|
| POST | `/v1/vehicles/{vehicleId}/maintenances` | Create maintenance |
| GET | `/v1/vehicles/{vehicleId}/maintenances` | List maintenances |
| GET | `/v1/vehicles/{vehicleId}/maintenances/{maintenanceId}` | Find maintenance |
| PUT | `/v1/vehicles/{vehicleId}/maintenances/{maintenanceId}` | Update maintenance |
| DELETE | `/v1/vehicles/{vehicleId}/maintenances/{maintenanceId}` | Delete maintenance |

## Cloud demo deployment preparation

This project includes a Kubernetes/EKS deployment foundation for a public AWS demo environment without changing the local Docker Compose flow.

Key files:

```text
frontend/Dockerfile
deploy/helm/autolog
.github/workflows
infra/terraform-bootstrap
infra/terraform
docs/deployment-aws-kubernetes.md
```

Read the deployment guide:

```text
docs/deployment-aws-kubernetes.md
```

The public demo environment is the target for portfolio usage. Production deployment is intentionally out of scope for now.

## Configuration

Main configuration file:

```text
src/main/resources/application.yml
```

Important environment variables:

```text
JWT_SECRET
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
APP_SECURITY_CORS_ALLOWED_ORIGINS
```

Default local database:

```text
database: vehicle-maintenance-history
username: vehicle-maintenance-history
password: vehicle-maintenance-history
```

## Database migrations

Flyway migrations are located at:

```text
src/main/resources/db/migration
```

## Observability

Every HTTP response includes:

```http
X-Request-Id
```

If the client sends this header, the API reuses it. Otherwise, the API generates a new one.

## Project structure

```text
com.mmetzner.vmh
|-- auth
|-- vehicle
|-- maintenance
`-- shared
```

Each feature is organized by:

```text
domain
application
infrastructure
presentation
```

## Out of scope

The following topics were intentionally left out for now:

- Outbox
- Kafka
- Production deployment
- Multi-environment promotion
- High availability beyond a small public demo environment
