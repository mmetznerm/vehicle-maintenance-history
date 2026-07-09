# Vehicle Maintenance History

REST API and frontend to manage vehicles and their maintenance history.

## Stack

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- JWT
- Docker
- Testcontainers
- Swagger/OpenAPI
- Spring Actuator
- React
- TypeScript
- Vite

## Project Structure

```text
vehicle-maintenance-history/
  backend/     Spring Boot API and static frontend output.
  frontend/    React + TypeScript + Vite source code.
  docs/        Project documentation.
```

Backend package structure:

```text
com.mmetzner.vmh
  auth
  vehicle
  maintenance
  shared
```

Each backend feature is organized by:

```text
domain
application
infrastructure
presentation
```

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

## Running Locally With Docker

```bash
docker compose --profile app up --build
```

Application:

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

## Recommended Local Development Workflow

For day-to-day development, prefer running only PostgreSQL in Docker and running
the Spring Boot application locally from the IDE. This keeps Java debugging,
breakpoints, hot reload and logs easier to use.

Start only the database:

```bash
docker compose up -d postgres
```

Build the frontend into Spring Boot static resources:

```bash
cd frontend
npm.cmd run build
```

Then start `VmhApplication` from the IDE. The full application is available from
a single origin:

```text
http://localhost:8080
```

Useful frontend routes:

```text
http://localhost:8080/login
http://localhost:8080/register
```

In IntelliJ IDEA, make the green Start button build the frontend before starting
the backend:

```text
Run/Debug Configurations > VmhApplication > Modify options > Add before launch task
```

Add an npm task pointing to:

```text
package.json: frontend/package.json
script: build
```

After that, pressing Start on `VmhApplication` refreshes the frontend build and
starts the backend.

For fast frontend-only iteration, run:

```bash
cd frontend
npm.cmd run dev
```

The Vite dev server proxies `/v1` requests to `http://localhost:8080`.

## Running Tests

Unit and controller tests:

```bash
cd backend
.\mvnw.cmd test
```

Integration tests:

```bash
cd backend
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

## Main Endpoints

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

## Configuration

Main backend configuration file:

```text
backend/src/main/resources/application.yml
```

Important environment variables:

```text
JWT_SECRET
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

Default local database:

```text
database: vehicle-maintenance-history
username: vehicle-maintenance-history
password: vehicle-maintenance-history
```

## Database Migrations

Flyway migrations are located at:

```text
backend/src/main/resources/db/migration
```

## Observability

Every HTTP response includes:

```http
X-Request-Id
```

If the client sends this header, the API reuses it. Otherwise, the API generates
a new one.

## Out Of Scope

The following topics were intentionally left out for now:

- Outbox
- Kafka
- Kubernetes
- Advanced CI/CD
