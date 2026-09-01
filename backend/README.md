# Nepal Hazard Watch Backend

Experimental research backend for Nepal Hazard Watch. This service is not an official emergency-warning system and must not be used to direct evacuations or send public alerts.

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 16+ (or Docker)

## Local PostgreSQL

From the repository root:

```bash
docker run --name nepal-hazard-watch-postgres \
  -e POSTGRES_DB=nepal_hazard_watch \
  -e POSTGRES_USER=nhw \
  -e POSTGRES_PASSWORD=nhw_local_dev \
  -p 5432:5432 \
  -d postgres:16
```

The default connection settings match the command above. Override them with `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` when needed.

## Run locally

From `backend/`:

```bash
mvn spring-boot:run
```

The application starts at `http://localhost:8080`. Health is available at `http://localhost:8080/actuator/health`.

## Test and package

```bash
mvn test
mvn -DskipTests package
```

Flyway migrations run automatically on application startup from `src/main/resources/db/migration`.
