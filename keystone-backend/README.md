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
Update `src/main/resources/application.properties` if your PostgreSQL username/password is different.

## Run
```bash
mvn spring-boot:run
```

## Auth endpoints
- POST `/api/auth/signup`
- POST `/api/auth/login`


