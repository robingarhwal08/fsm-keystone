# Exception Handling Debt Baseline — FSM Keystone

**Baseline date**: 2026-08-10  
**Baseline count**: **34 `RuntimeException` throw sites** across all backend source files  
**`@ControllerAdvice` handlers present**: **0** (none)  
**Related**: [layer-violation-triage.md](layer-violation-triage.md) | [coupling-metrics.md](coupling-metrics.md)

---

## Absent global exception handler

There is **no `@RestControllerAdvice` or `@ControllerAdvice` class** in `com.fsm.keystone`. The `exception/` package contains custom exception subclasses (`ApiException`, `ResourceNotFoundException`, `BusinessRuleException`, etc.) and an `ErrorCode` enum whose Javadoc references a "`@RestControllerAdvice`", but the handler itself has not been implemented.

**Risk**: When a service method throws `RuntimeException` with a message like `"Customer not found with id: 42"` or `"Email already exists"`, Spring's default exception handling in Spring Boot 4 falls through to `BasicErrorController` and returns a generic `500 Internal Server Error`. The raw exception message, and potentially a full stack trace (`server.error.include-stacktrace`), may reach the client. This exposes:

- **Internal resource IDs** (`"Work order not found with id: 7"` tells the client the ID exists/doesn't exist)
- **Email enumeration** (`"Email already exists"` in `AuthService.signup` allows username enumeration)
- **Stack traces** in non-production profiles (if `server.error.include-stacktrace=always` is set)

**Remediation**: A `@RestControllerAdvice` handler that maps each `ApiException` subclass to an appropriate HTTP status and structured error body (see `ApiException.java` and `ErrorCode.java` for the intended structure). `RuntimeException` sites should be replaced with typed exceptions from the existing hierarchy.

---

## Throw-site inventory (34 sites)

### ApplicationConfig (`com.fsm.keystone.config`)

| # | File | Line | Method | Message | HTTP risk if unhandled |
|---|---|---|---|---|---|
| 1 | `ApplicationConfig.java` | 24 | `userDetailsService` (lambda) | `"User not found"` | 500 — authentication fails with opaque error |

### AuthService (`com.fsm.keystone.service`)

| # | File | Line | Method | Message | HTTP risk if unhandled |
|---|---|---|---|---|---|
| 2 | `AuthService.java` | 30 | `signup` | `"Email already exists"` | 500 — **email enumeration risk**: exposes whether an address is registered |
| 3 | `AuthService.java` | 35 | `signup` (lambda) | `"Customer not found"` | 500 — internal ID exposure |
| 4 | `AuthService.java` | 73 | `login` (lambda) | `"User not found"` | 500 — **user enumeration risk** when credential is correct but post-auth lookup fails |

### CustomerService (`com.fsm.keystone.service`)

| # | File | Line | Method | Message | HTTP risk if unhandled |
|---|---|---|---|---|---|
| 5 | `CustomerService.java` | 26 | `getCustomerById` (lambda) | `"Customer not found with id: " + id` | 500 — internal ID in error message |
| 6 | `CustomerService.java` | 32 | `updateCustomer` (lambda) | `"Customer not found with id: " + id` | 500 |
| 7 | `CustomerService.java` | 45 | `deleteCustomer` (lambda) | `"Customer not found with id: " + id` | 500 |

### PartService (`com.fsm.keystone.service`)

| # | File | Line | Method | Message | HTTP risk if unhandled |
|---|---|---|---|---|---|
| 8 | `PartService.java` | 27 | `getPartById` (lambda) | `"Part not found with id : " + id` | 500 |
| 9 | `PartService.java` | 34 | `updatePart` (lambda) | `"Part not found with id : " + id` | 500 |
| 10 | `PartService.java` | 50 | `deletePart` (lambda) | `"Part not found with id : " + id` | 500 |

### SiteService (`com.fsm.keystone.service`)

| # | File | Line | Method | Message | HTTP risk if unhandled |
|---|---|---|---|---|---|
| 11 | `SiteService.java` | 26 | `createSite` (lambda) | Customer lookup — "not found" | 500 |
| 12 | `SiteService.java` | 54 | `updateSite` (lambda) | Site lookup — "not found" | 500 |
| 13 | `SiteService.java` | 61 | `updateSite` (lambda) | Customer lookup in update — "not found" | 500 |
| 14 | `SiteService.java` | 95 | `deleteSite` (lambda) | Site lookup — "not found" | 500 |

### UserService (`com.fsm.keystone.service`)

| # | File | Line | Method | Message | HTTP risk if unhandled |
|---|---|---|---|---|---|
| 15 | `UserService.java` | 39 | `updateUser` (lambda) | `"User not found"` | 500 |
| 16 | `UserService.java` | 55 | `deleteUser` (lambda) | `"User not found"` | 500 |

### WorkOrderService (`com.fsm.keystone.service`)

| # | File | Line | Method | Message | HTTP risk if unhandled |
|---|---|---|---|---|---|
| 17 | `WorkOrderService.java` | 50 | `createWorkOrder` (lambda) | `"Customer not found with id: " + id` | 500 |
| 18 | `WorkOrderService.java` | 54 | `createWorkOrder` (lambda) | `"Site not found with id: " + id` | 500 |
| 19 | `WorkOrderService.java` | 60 | `createWorkOrder` (lambda) | `"User not found with id: " + id` | 500 |
| 20 | `WorkOrderService.java` | 66 | `createWorkOrder` (lambda) | `"Technician not found with id: " + id` | 500 |
| 21 | `WorkOrderService.java` | 92 | `getWorkOrderById` (lambda) | `"Work order not found with id: " + id` | 500 |
| 22 | `WorkOrderService.java` | 99 | `assignTechnician` (lambda) | `"Work order not found with id: " + id` | 500 |
| 23 | `WorkOrderService.java` | 103 | `assignTechnician` (lambda) | `"Technician not found with id: " + id` | 500 |
| 24 | `WorkOrderService.java` | 129 | `updateStatus` (lambda) | `"Work order not found with id: " + id` | 500 |
| 25 | `WorkOrderService.java` | 141 | `updateStatus` (lambda) | `"User not found with id: " + id` | 500 |
| 26 | `WorkOrderService.java` | 168 | `addPartUsage` (lambda) | `"Work order not found with id: " + id` | 500 |
| 27 | `WorkOrderService.java` | 172 | `addPartUsage` (lambda) | `"Part not found with id: " + id` | 500 |
| 28 | `WorkOrderService.java` | 196 | `addPartUsage` (lambda) | `"User not found with id: " + id` | 500 |
| 29 | `WorkOrderService.java` | 214 | `addTimeLog` (lambda) | `"Work order not found with id: " + id` | 500 |
| 30 | `WorkOrderService.java` | 218 | `addTimeLog` (lambda) | `"Technician not found with id: " + id` | 500 |
| 31 | `WorkOrderService.java` | 251 | `updateWorkOrder` (lambda) | `"Work Order not found"` | 500 |
| 32 | `WorkOrderService.java` | 260 | `updateWorkOrder` (lambda) | `"Customer not found"` | 500 |
| 33 | `WorkOrderService.java` | 267 | `updateWorkOrder` (lambda) | `"Site not found"` | 500 |
| 34 | `WorkOrderService.java` | 276 | `updateWorkOrder` (lambda) | `"Technician not found"` | 500 |

---

## Count note (baseline vs. WO estimate)

The work order description estimated "21 call sites". The current count is **34**. The additional 13 sites are concentrated in `WorkOrderService.updateWorkOrder` (lines 251–276, 4 sites) and `WorkOrderService.createWorkOrder` (lines 50–66, 4 sites), both of which were present in the code at the time of analysis. The WO estimate likely reflects a snapshot of the WorkOrderService taken before the `updateWorkOrder` method was fully fleshed out. The authoritative count for this baseline is **34**.

---

## Remediation priority

| Severity | Sites | Notes | Fix-in epic |
|---|---|---|---|
| **Critical** | 2, 4 (email/user enumeration) | Must return generic 401 message from `@ControllerAdvice`; raw message must never reach client | Phase 2 (global error handler) |
| **High** | All 34 | Replace `RuntimeException` with typed exceptions from `ApiException` hierarchy; handle in `@RestControllerAdvice` | Phase 2 (global error handler) |
| **High** | All 34 | Implement `@RestControllerAdvice` mapping each exception to structured `ErrorResponse` with HTTP status, `code`, `message`, `correlationId` | Phase 2 (global error handler) |

> **Target state**: zero bare `RuntimeException` throw sites; all exceptions are typed subclasses of `ApiException` handled by a `@RestControllerAdvice` that maps to structured HTTP responses.
