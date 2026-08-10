# Field Service Management Backend

## Requirements
- Java 22 or higher version
- Maven
- PostgreSQL

## Database
Create database:
```sql
CREATE DATABASE fsmdb;
```

```sql
show databases;
```
Supply credentials via environment variables — **do not edit `application.properties` with real values**.
Copy `../.env.example` to `../.env` and fill in `SPRING_DATASOURCE_PASSWORD` and `APP_JWT_SECRET`.
See [`../docs/baseline/required-properties.md`](../docs/baseline/required-properties.md) for the full manifest.

## Run
```bash
mvn spring-boot:run
```

## Auth endpoints
- POST `/api/auth/signup`
- POST `/api/auth/login`


