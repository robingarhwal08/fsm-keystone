# Field Service Management System Project Developed by Robin


- Backend: Spring Boot 3, Java 21, JWT Security, PostgreSQL, JPA
- Frontend: React + Vite

## Run order

### 1. PostgreSQL
```sql
CREATE DATABASE fsmdb;
```

If your PostgreSQL user/password is not `postgres/postgres`, update:
`backend/src/main/resources/application.properties`

### 2. Keystone-Backend
```bash
cd keystone-backend
mvn spring-boot:run
```
or 
```bash
cd keystone-backend
.\mvnw spring-boot:run
```

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```

### 4. Start testing
1. Signup as MANAGER/DISPATCHER/TECHNICIAN/CUSTOMER.
2. Create customers.
3. Create parts.
4. Create sites from the Sites page.
5. Create and update work orders from the UI.

