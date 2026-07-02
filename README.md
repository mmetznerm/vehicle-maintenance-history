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