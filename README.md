# Vehicle Maintenance History

[![PR Checks](https://github.com/mmetznerm/vehicle-maintenance-history/actions/workflows/pr-checks.yml/badge.svg)](https://github.com/mmetznerm/vehicle-maintenance-history/actions/workflows/pr-checks.yml)
[![CodeQL](https://github.com/mmetznerm/vehicle-maintenance-history/actions/workflows/codeql.yml/badge.svg)](https://github.com/mmetznerm/vehicle-maintenance-history/actions/workflows/codeql.yml)
[![Security](https://github.com/mmetznerm/vehicle-maintenance-history/actions/workflows/security.yml/badge.svg)](https://github.com/mmetznerm/vehicle-maintenance-history/actions/workflows/security.yml)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
![React](https://img.shields.io/badge/React-19-61DAFB)
![Coverage Gate](https://img.shields.io/badge/Coverage%20Gate-60%25-success)

Event-driven application to manage vehicle maintenance and publish a privacy-safe,
revocable digital history for each vehicle.

## Stack

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Apache Kafka
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
  backend/       Transactional API, source database and Kafka outbox producer.
  event-worker/  Kafka consumer and public-history read model.
  consistency-worker/ Kafka consumer, rule engine and alert outbox producer.
  frontend/      React + TypeScript + Vite source code.
  contracts/     Versioned event schemas and examples.
  docs/          Project documentation.
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
- Revocable public sharing links with identifier rotation
- Versioned vehicle and maintenance events on Kafka
- Transactional outbox with at-least-once delivery
- Idempotent public-history projection in an independent database
- Asynchronous detection of odometer rollback, duplicate records and impossible dates
- Alert lifecycle events and an authenticated inconsistency projection
- Public history without owner, license plate or maintenance cost
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

Public-history worker API:

```text
http://localhost:8081
```

Consistency-worker health endpoint:

```text
http://localhost:8082/actuator/health
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

For day-to-day development, prefer running PostgreSQL and Kafka in Docker and running
the Spring Boot application locally from the IDE. This keeps Java debugging,
breakpoints, hot reload and logs easier to use.

Start the local infrastructure (the three PostgreSQL databases and Kafka):

```bash
docker compose up -d postgres history-postgres consistency-postgres kafka
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

Start `VehicleHistoryWorkerApplication` from the IDE as well when exercising
public history. The worker listens on `http://localhost:8081`.

Start `MaintenanceConsistencyWorkerApplication` to exercise inconsistency
detection. It listens on `http://localhost:8082` and consumes the same source
topic with an independent consumer group.

Useful frontend routes:

```text
http://localhost:8080/login
http://localhost:8080/register
http://localhost:8080/history/{publicId}
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

Event-worker unit and Kafka integration tests:

```bash
cd backend
.\mvnw.cmd -f ..\event-worker\pom.xml verify
.\mvnw.cmd -f ..\event-worker\pom.xml verify -Pintegration-tests
```

Consistency-worker unit and Kafka/PostgreSQL integration tests:

```bash
cd backend
.\mvnw.cmd -f ..\consistency-worker\pom.xml verify
.\mvnw.cmd -f ..\consistency-worker\pom.xml verify -Pintegration-tests
```

## CI Quality Gates

Pull requests are checked with:

- Backend unit, controller and coverage checks with Maven and JaCoCo.
- Backend integration tests with Testcontainers and PostgreSQL.
- Event-worker unit tests and Kafka/PostgreSQL integration tests.
- Consistency-worker rule, reconciliation and Kafka/PostgreSQL integration tests.
- Frontend lint, Vitest coverage and production build.
- OpenAPI contract export as a workflow artifact.
- CodeQL, Dependency Review and Trivy security scans.

Coverage reports and the generated OpenAPI document are uploaded as GitHub
Actions artifacts.

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
| GET | `/v1/vehicles/{vehicleId}/history-sharing` | Read sharing status |
| POST | `/v1/vehicles/{vehicleId}/history-sharing` | Enable public history |
| DELETE | `/v1/vehicles/{vehicleId}/history-sharing` | Revoke public history |
| GET | `/v1/vehicles/{vehicleId}/inconsistencies` | List active consistency alerts |

Set `includeResolved=true` on the inconsistency endpoint to include the complete
alert lifecycle. The endpoint applies the same ownership check as the vehicle
and maintenance APIs.

### Maintenances

| Method | Endpoint | Description |
|---|---|---|
| POST | `/v1/vehicles/{vehicleId}/maintenances` | Create maintenance |
| GET | `/v1/vehicles/{vehicleId}/maintenances` | List maintenances |
| GET | `/v1/vehicles/{vehicleId}/maintenances/{maintenanceId}` | Find maintenance |
| PUT | `/v1/vehicles/{vehicleId}/maintenances/{maintenanceId}` | Update maintenance |
| DELETE | `/v1/vehicles/{vehicleId}/maintenances/{maintenanceId}` | Delete maintenance |

### Public history

| Method | Endpoint | Description |
|---|---|---|
| GET | `http://localhost:8081/v1/public/vehicle-histories/{publicId}` | Read the sanitized public history |

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
SPRING_KAFKA_BOOTSTRAP_SERVERS
SPRING_KAFKA_CONSUMER_GROUP_ID
KAFKA_VEHICLE_MAINTENANCE_TOPIC
KAFKA_MAINTENANCE_ALERT_TOPIC
KAFKA_OUTBOX_POLL_INTERVAL
KAFKA_OUTBOX_BATCH_SIZE
VITE_HISTORY_API_BASE_URL
```

The default Kafka bootstrap server is `localhost:29092` when the backend runs
locally and `kafka:9092` when it runs with the Docker Compose `app` profile.

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
event-worker/src/main/resources/db/migration
consistency-worker/src/main/resources/db/migration
```

## Observability

Every HTTP response includes:

```http
X-Request-Id
```

If the client sends this header, the API reuses it. Otherwise, the API generates
a new one.

## Out Of Scope

See [`docs/event-driven-architecture.md`](docs/event-driven-architecture.md) for
the delivery guarantees, privacy boundary, replay procedure and demo flow.
See [`docs/maintenance-inconsistency-detection.md`](docs/maintenance-inconsistency-detection.md)
for rule semantics, alert lifecycle and a focused portfolio demonstration.

The following topics were intentionally left out for now:

- Kubernetes
- Advanced CI/CD
- Schema Registry
- Multi-broker Kafka deployment
