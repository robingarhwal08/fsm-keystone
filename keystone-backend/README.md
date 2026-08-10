# Field Service Management Backend

## Requirements

- **Java 21** (matches `java.version` in `pom.xml`; Java 22+ is not required)
- Maven 3.9+ or use the included Maven Wrapper (`./mvnw` / `.\mvnw.cmd`)
- PostgreSQL 16 (or use `docker compose up` — the database is created automatically)

## Environment Setup

**Never edit `application.properties` with real credentials.**
All secrets are supplied via environment variables or Docker Compose secrets.

1. Copy the template and generate secrets:
   ```bash
   bash ../scripts/dev/bootstrap-secrets.sh
   ```
   This creates a git-ignored `.env` and `secrets/` directory in the project root.

2. See [`../docs/baseline/required-properties.md`](../docs/baseline/required-properties.md)
   for the full list of required environment variables and which have no default.

## Database (non-Docker only)

If you are **not** using `docker compose up`, create the database manually:

```sql
CREATE DATABASE fsmdb;
```

## Run

Using the Maven Wrapper (recommended — no local Maven installation required):

```bash
./mvnw spring-boot:run
```

**Windows:**

```bash
.\mvnw.cmd spring-boot:run
```

Or with a locally installed Maven:

```bash
mvn spring-boot:run
```

The backend starts on port **8080** by default (`server.port=${PORT:8080}`).

## Tests

```bash
# Unit and slice tests (no Docker required)
./mvnw test

# Full suite including integration tests (Docker required for Testcontainers)
./mvnw verify

# Skip integration tests in Docker-less environments
./mvnw verify -DskipITs=true
```

See [`../docs/testing.md`](../docs/testing.md) for test categories and coverage policy.

## Auth endpoints

- `POST /api/auth/signup`
- `POST /api/auth/login`
