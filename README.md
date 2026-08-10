# Field Service Management System

A full-stack **Field Service Management System** developed by Robin.


The objective of the Field Service Management (FSM) project is to build a centralized platform that helps organizations manage field service operations efficiently. It allows customers to create service requests, dispatchers to assign work orders, technicians to update job progress, and managers to monitor performance and Service Level Agreements (SLAs). The system improves communication, reduces manual work, tracks service activities in real time, and increases overall operational efficiency.


## Project Demo Link :  https://www.youtube.com/live/G3ixhgYTLrM?si=prSdeHZMieKxUim4

## Project Deployed at (Live Now) : https://fsm-keystone-bf2v.vercel.app/

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

## Security Notice

> **Important:** The default configuration files contain placeholder credentials that were
> historically committed to this repository and are now considered compromised.
> **Never use the default values in any environment** — always supply secrets via environment
> variables. See:
> - [`docs/baseline/required-properties.md`](docs/baseline/required-properties.md) — required env variable manifest
> - [`docs/runbooks/secret-rotation.md`](docs/runbooks/secret-rotation.md) — rotation procedure
> - [`.env.example`](.env.example) — environment variable template (copy to `.env` and fill in real values)

---

## Step 1: Create the PostgreSQL Database

Run the following SQL command:

```sql
CREATE DATABASE fsmdb;
```

Supply database credentials via environment variables (see `.env.example`).
**Do not edit `application.properties` with real passwords** — use the `.env` file instead.

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
