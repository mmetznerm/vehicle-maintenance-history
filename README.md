# Vehicle Maintenance History

[![CI/CD](https://github.com/mmetznerm/vehicle-maintenance-history/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/mmetznerm/vehicle-maintenance-history/actions/workflows/ci-cd.yml)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
![React](https://img.shields.io/badge/React-19-61DAFB)
![Coverage Gate](https://img.shields.io/badge/Coverage%20Gate-60%25-success)

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
docker compose up --build
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

## Docker Hub

Successful pushes to `main` publish the application image to Docker Hub after
all CI checks pass. This delivery step does not deploy the application to a
server.

Pull the latest successful build:

```bash
docker pull mmetznerm/vehicle-maintenance-history:latest
```

Published tags:

- `latest` points to the newest successful build from `main`.
- `sha-<commit>` identifies the exact source commit used for the image.

## AWS Deployment

The [AWS deployment guide](docs/aws-deployment.md) describes the console setup,
the first bootstrap and day-to-day operations. Docker Hub remains the public
portfolio registry, while Amazon ECR is the private runtime registry read by
EC2. GitHub Actions authenticates to AWS through GitHub OIDC instead of keeping
persistent AWS keys in the repository: publishing and deployment assume
different OIDC roles. After CI passes, the workflow publishes an immutable ECR
`sha-<commit>` image and SSM deploys that tag to EC2, which uses encrypted EBS
and reaches the private PostgreSQL RDS instance through its security-group
reference. For audit, operations record the ECR digest associated with each
deployed SHA tag, while deployment continues to use the immutable SHA tag.

```text
GitHub -> Actions -> Docker Hub + ECR -> SSM -> EC2 -> RDS
```

The public demonstration uses HTTP and test data only; HTTPS and production
data are outside this portfolio deployment.

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

## CI Quality Gates

Pull requests are checked with:

- Backend unit, controller and coverage checks with Maven and JaCoCo.
- Backend integration tests with Testcontainers and PostgreSQL.
- Frontend lint, Vitest coverage and production build.
- CodeQL security analysis, a direct publication gate.
- `deployment-contracts`, ShellCheck, actionlint and Compose rendering.

After these checks pass on a push to `main`, GitHub Actions builds the production
image and publishes it to Docker Hub and ECR with `latest` and `sha-<commit>`
tags. The publish job assumes the ECR-only OIDC role, while the deploy job
assumes the SSM-only OIDC role; ECR SHA tags are immutable.

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

The following topics remain outside the current portfolio scope:

- Outbox
- Kafka
- Terraform or CloudFormation
- ECS, EKS, Kubernetes or multiple EC2 instances
- Application Load Balancer or Auto Scaling
- Route 53, custom domain or HTTPS
- RDS Multi-AZ, Aurora or RDS Proxy
- SSH access
- Automatic rollback
- GitHub Environments and deployment approvals
