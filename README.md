# Vehicle Maintenance History

Backend for managing vehicles and their maintenance histories.

## Tecnologies

- Java 21
- Spring Boot 3.5
- Maven

## Executing tests

```powershell```
.\mvnw.cmd test

## Docker

Start only PostgreSQL:

```powershell```
docker compose up -d postgres

Start the application and PostgreSQL:

```powershell```
docker compose --profile app up --build

```markdown```
## API documentation

With the application running:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

API contracts and standardized errors are documented in
[`docs/api-contracts.md`](docs/api-contracts.md).