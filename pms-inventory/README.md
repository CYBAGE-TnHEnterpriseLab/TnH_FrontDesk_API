# PMS Inventory Microservice

Room-type-level inventory microservice for PMS.

## Tech stack

- Java 21
- Spring Boot 3.5.3
- PostgreSQL
- Spring Data JPA
- Flyway
- MapStruct
- Lombok
- Springdoc OpenAPI
- JUnit 5, Mockito, Testcontainers

## Run locally

1. Create database `pms_inventory` in PostgreSQL.
2. Set environment variables if needed (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`).
3. Run:

```powershell
mvn clean spring-boot:run
```

## Test

```powershell
mvn clean test
```

Integration tests use PostgreSQL Testcontainers.

## OpenAPI

- API docs: `/v3/api-docs`
- Swagger UI: `/swagger-ui.html`

