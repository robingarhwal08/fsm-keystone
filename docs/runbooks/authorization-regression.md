# Authorization Regression Triage Runbook

| Field | Value |
|-------|-------|
| Owner | Security reviewer + Backend engineer |
| Last rehearsed | 2026-08-10 (see [rehearsals.md](rehearsals.md)) |

---

## Trigger

- Spike in HTTP 401 or 403 responses in the monitoring dashboard.
- Users report "Access Denied" or blank screens after a deploy.
- Alert fires on authorization error rate crossing the SLO threshold.
- Post-deploy verification finds unexpectedly denied requests.

---

## Architecture reference

- **Layer 1 — Filter chain** (`SecurityConfig.securityFilterChain`): routes matching `permitAll()` bypass the JWT filter; all others require a valid bearer token. See [ADR-0001](../adr/0001-default-deny-security-filter-chain.md).
- **Layer 2 — Method security** (`@PreAuthorize` on controller methods): role-based decisions after authentication. See [ADR-0002](../adr/0002-method-security-preauthorize-archunit-gate.md).
- **Layer 3 — Tenancy scoping** (service layer, `AppUser.customer`): unscoped `findAll()` calls return all-tenant data; client-side filtering compensates until server-side scoping is applied. See [ADR-0003](../adr/0003-service-layer-tenancy-scoping.md).

---

## Symptom-to-cause map

### Symptom A: HTTP 401 — Unauthorized

**Meaning**: No valid JWT token was presented, or the token was rejected.

**Triage steps**:

```bash
# 1. Check the request includes the Authorization header
curl -v http://localhost:8080/api/work-orders \
  -H "Authorization: Bearer <token>"

# 2. Check the backend log for JwtAuthenticationFilter output
docker compose logs springboot-app | grep -E "JWT|token|authentication|401"
```

| Sub-cause | Distinguishing log or behaviour | Remediation |
|-----------|--------------------------------|-------------|
| Missing header | No `Authorization` header in request; filter returns 401 without log | Client must include `Authorization: Bearer <token>` |
| Expired token | `io.jsonwebtoken.ExpiredJwtException` in log | Client must re-authenticate; server TTL is `app.jwt.expiration-ms` |
| Invalid signature | `io.jsonwebtoken.security.SecurityException` in log | Token forged or signed with wrong key; re-authenticate; if widespread, check for key rotation event |
| Token for wrong environment | Decoded payload shows wrong `sub` or environment hint | Client connected to wrong environment |

**Log field to inspect**: `JwtAuthenticationFilter.doFilterInternal` → the `jwt` variable and the `extractUsername` call.

---

### Symptom B: HTTP 403 — Forbidden (filter-chain source)

**Meaning**: The request passed JWT validation but was denied by the `authorizeHttpRequests` rules.

**Triage steps**:

```bash
# 1. Identify whether the route is in a permitAll group or the anyRequest catch-all
# Current permitAll groups (SecurityConfig.java):
#   /api/auth/**  (public)
#   /api/users/** (filter-chain gap — see ADR-0001)
#   /api/customers/**
#   /api/time-logs/**
#   /api/part-usage/**
#   All others: anyRequest().authenticated()

# 2. Confirm the token is valid (Step A above) — a 403 from the filter chain only
#    occurs after a valid token is present (otherwise it would be 401).

# 3. Check if a new route was added without adding a permitAll or authenticated() rule
grep "requestMatchers\|permitAll\|authenticated" \
  keystone-backend/src/main/java/com/fsm/keystone/security/SecurityConfig.java
```

| Sub-cause | Distinguishing feature | Remediation |
|-----------|------------------------|-------------|
| New route added without a rule | Route not in `authorizeHttpRequests`; `anyRequest().authenticated()` catch-all applies | Add appropriate `requestMatchers` rule in `SecurityConfig` |
| Route incorrectly classified as requiring auth | Route is not public but should be | Add `permitAll()` rule or confirm this is correct and the client must authenticate |

---

### Symptom C: HTTP 403 — Forbidden (@PreAuthorize source)

**Meaning**: Authentication succeeded and the filter chain allowed the request, but the method-level `@PreAuthorize` expression denied it.

**Triage steps**:

```bash
# 1. Find the controller method and its @PreAuthorize expression
grep -n "@PreAuthorize\|@GetMapping\|@PostMapping\|@PutMapping\|@PatchMapping\|@DeleteMapping" \
  keystone-backend/src/main/java/com/fsm/keystone/controller/WorkOrderController.java

# 2. Decode the JWT and confirm the role claim
# The subject claim is the user's email; role is carried in authorities
docker compose logs springboot-app | grep -E "403|PreAuthorize|AccessDenied"

# 3. Check if the method has NO @PreAuthorize annotation (silent denial failure mode)
# A handler with no @PreAuthorize and a route in anyRequest().authenticated() will
# return 403 if the spring.security.method.access-denied-exception-if-denied=true
# setting is active, or may silently deny.
```

**Silent-403 unannotated-handler failure mode**: If a controller method has no `@PreAuthorize` annotation and the filter-chain `anyRequest().authenticated()` rule applies, the request may succeed (return 200) for authenticated users, which is correct. However, if `@EnableMethodSecurity` combined with a parent-class `@PreAuthorize` causes unexpected inheritance, the handler may return 403 with no obvious log cause. Check the `EndpointAuthorizationInventoryTest` report (`target/endpoint-authorization-inventory.txt`) for any endpoint listed as `NONE` that should have role restrictions.

| Sub-cause | Distinguishing log | Remediation |
|-----------|-------------------|-------------|
| Wrong role | `AccessDeniedException` in log; user's role does not match expression | Verify the user's role assignment in the `app_user` table |
| Missing @PreAuthorize | Endpoint shows `NONE` in inventory; request denied silently | Add `@PreAuthorize` to the handler method |
| `@PreAuthorize` commented out | `CustomerController.delete` (known gap, ADR-0002) | Re-enable annotation after security review |

---

### Symptom D: Empty result screen (tenancy-scoping source)

**Meaning**: The request returns HTTP 200 with an empty list. No error is thrown, but the user sees no data.

**Triage steps**:

```bash
# 1. Confirm the endpoint returns data for a MANAGER (no tenant filter applied to managers)
curl http://localhost:8080/api/work-orders \
  -H "Authorization: Bearer <manager-token>"

# 2. Compare with a CUSTOMER token (client-side filter applies)
curl http://localhost:8080/api/work-orders \
  -H "Authorization: Bearer <customer-token>"

# 3. Check whether both server-side and client-side scoping are active (double-filter)
# Server: WorkOrderService.getAllWorkOrders() calls workRepo.findAll() (no server-side filter yet)
# Client: Dashboard.jsx and CustomerRequests.jsx filter by user.customerId
# If customerId is null or mismatched, the client filter returns empty even though the API
# returned data.
```

**Double-filter symptom**: when both server-side tenancy scoping and client-side filtering are active simultaneously, customers may see empty screens even though data exists in the database. Confirm by:
1. Checking `AppUser.customer_id` is correctly set for the user in the `app_user` table.
2. Checking the JWT-decoded `sub` (email) resolves to the correct `AppUser` record.
3. Checking `Dashboard.jsx:49-60` — the client filter uses `user.customerId`; if the JWT payload does not carry `customerId`, the filter drops all results.

| Sub-cause | Distinguishing feature | Remediation |
|-----------|------------------------|-------------|
| User has no customer association | `AppUser.customer_id IS NULL` | Assign the user to a customer in the database |
| JWT does not carry customerId | Dashboard filter uses `user.customerId` from JWT decode | Backend must include customerId in the JWT claims |
| Server-side scope not yet applied | `WorkOrderService.getAllWorkOrders()` calls `findAll()` | This is the documented gap (ADR-0003); client-side filter is the current mitigation |

---

## Structured error body reference

When the global exception handler is active (`@RestControllerAdvice`), errors carry:

```json
{
  "timestamp": "2026-08-10T14:32:01Z",
  "status": 403,
  "code": "CROSS_TENANT_ACCESS_DENIED",
  "message": "Access to this resource is not permitted",
  "correlationId": "a3f2b1c9-...",
  "fieldErrors": []
}
```

The `correlationId` is the **join key** between an alert and the log line. Use it to find the full stack trace:

```bash
docker compose logs springboot-app | grep "a3f2b1c9-..."
```

**Fallback when no correlationId** (request failed before the error handler ran): correlate by `timestamp` + `container_id` + HTTP path in the access log.

---

## Verification

After applying a fix:

```bash
# 1. 401: re-authenticate and retry with new token → expect 200
# 2. 403 filter: add rule and restart backend → confirm endpoint returns 200 for authenticated user
# 3. 403 annotation: add @PreAuthorize, rebuild, deploy → confirm correct roles succeed and others get 403
# 4. Empty screen: fix user's customer_id → re-call endpoint and confirm data appears
```

**Pass criterion**: the reported symptom no longer reproduces; no new 401/403 wave appears in monitoring.

---

## Escalation

| Condition | Escalate to | Maximum time |
|-----------|-------------|-------------|
| Widespread 401 wave (> 5% of requests) | Backend engineer | 15 minutes |
| Suspected JWT key compromise | Security reviewer | Immediate — trigger secret-rotation runbook |
| Silent-403 for a privileged endpoint | Security reviewer + Backend engineer | 30 minutes |
| Data-isolation breach (cross-tenant data visible) | Security reviewer | Immediate |

> **Prohibited**: never re-enable `permitAll()` for a route as an incident workaround. The sanctioned alternative is to add the correct `@PreAuthorize` expression or adjust the role assignment.
