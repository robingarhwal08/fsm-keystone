# Field Service Management System

A full-stack **Field Service Management System** developed by Robin.

## Technology Stack

### Backend

* Spring Boot 3
* Java 21
* Spring Security with JWT Authentication
* Hibernate 
* Validation
* PostgreSQL
* Spring Data JPA

### Frontend

* React
* Vite

---

# Project Setup

## Step 1: Create the PostgreSQL Database

Run the following SQL command:

```sql
CREATE DATABASE fsmdb;
```

If your PostgreSQL username or password is different from `postgres/postgres`, update the database configuration in:

```
keystone-backend/src/main/resources/application.properties
```

---

## Step 2: Start the Backend

Navigate to the backend directory:

```bash
cd keystone-backend
```

Run the application using Maven:

```bash
mvn spring-boot:run
```

Or, if using the Maven Wrapper:

```bash
./mvnw spring-boot:run
```

**For Windows**

```bash
.\mvnw spring-boot:run
```

The backend will start on the configured server port (default: **8080**).

---

## Step 3: Start the Frontend

Navigate to the frontend directory:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

The frontend will typically be available at:

```
http://localhost:5173
```

---

# Application Workflow

After both the backend and frontend are running:

1. Register a new account as one of the following roles:

    * MANAGER
    * DISPATCHER
    * TECHNICIAN
    * CUSTOMER

2. Log in with your account.

3. Create customer records.

4. Add inventory parts.

5. Create customer sites from the **Sites** page.

6. Create, assign, and update work orders.

7. Track work progress, time logs, and part usage.

---

# User Roles & Permissions

The application implements **Role-Based Access Control (RBAC)** using JWT authentication.

### Manager

* Full access to the system
* Manage users
* Manage customers
* Manage sites
* Manage parts inventory
* Create and assign work orders
* View reports and dashboards

### Dispatcher

* Create and assign work orders
* Manage sites
* Schedule technicians
* Monitor work order status

### Technician

* View assigned work orders
* Update work order progress
* Log working hours
* Record parts used
* Complete assigned jobs

### Customer

* View their own service requests
* Track work order status
* Access reports related to their requests

---

# Features

* JWT Authentication & Authorization
* Secure Role-Based Access Control
* Customer Management
* Site Management
* Work Order Management
* Parts Inventory Management
* Technician Assignment
* Time Logging
* Part Usage Tracking
* Customer Service Requests
* Reports Dashboard
* Responsive React Frontend
* PostgreSQL Database
* RESTful APIs using Spring Boot

---

# Tech Stack Summary

| Component      | Technology      |
| -------------- | --------------- |
| Frontend       | React + Vite    |
| Backend        | Spring Boot 3   |
| Language       | Java 21         |
| Authentication | JWT             |
| Database       | PostgreSQL      |
| ORM            | Spring Data JPA |
| Build Tool     | Maven           |

---

# Notes

* Ensure PostgreSQL is running before starting the backend.
* Update the database credentials in `application.properties` if required.
* Start the backend before launching the frontend.
* Different user roles have different permissions and access levels throughout the application.
