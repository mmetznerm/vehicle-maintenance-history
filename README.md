# Vehicle Maintenance History

API REST para gerenciamento de veículos e seus históricos de manutenção.

O projeto está sendo construído por features, começando pela infraestrutura e autenticação.

## Tecnologias

- Java 21
- Spring Boot 3.5
- Maven Wrapper
- PostgreSQL 16
- Flyway
- Spring Data JPA
- Spring Security
- JWT
- Docker e Docker Compose
- OpenAPI e Swagger UI
- JUnit, Mockito e AssertJ
- Testcontainers

## Requisitos

Para executar localmente com Java:

- Java 21
- Docker Desktop

Não é necessário instalar Maven ou PostgreSQL.

O Maven Wrapper está incluído no projeto:

```powershell
.\mvnw.cmd
```

## Estrutura

O código é organizado por contexto de negócio e por camadas:

```text
src/main/java/com/mmetzner/vmh/
├── auth/
│   ├── application/
│   ├── domain/
│   ├── infrastructure/
│   └── presentation/
└── shared/
    ├── common/
    ├── config/
    ├── exception/
    └── presentation/
```

Responsabilidades:

- `domain`: modelos e contratos independentes de infraestrutura.
- `application`: casos de uso e DTOs.
- `infrastructure`: banco, JPA, JWT e configurações técnicas.
- `presentation`: controllers e contratos HTTP.
- `shared`: componentes utilizados por mais de uma feature.

## Banco de dados

O PostgreSQL local utiliza:

```text
Database: vehicle-maintenance-history
Username: vehicle-maintenance-history
Password: vehicle-maintenance-history
Port: 5432
```

Essas credenciais são exclusivas para desenvolvimento local.

As alterações no banco são controladas pelo Flyway:

```text
src/main/resources/db/migration/
├── V1__bootstrap.sql
├── V2__create_users