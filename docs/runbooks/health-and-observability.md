# Health and Observability Runbook

| Field | Value |
|-------|-------|
| Owner | On-call responder (any role) |
| Last rehearsed | 2026-08-10 (see [rehearsals.md](rehearsals.md)) |

---

## Trigger

- An alert fires and the service state is unknown.
- Users report that the application is unreachable or returning errors.
- A monitoring check shows a container is unhealthy or restarting.
- An incident requires request tracing through the stack.

---

## Container health overview

| Container | Healthcheck type | Command | Interval / Retries / Timeout |
|-----------|-----------------|---------|------------------------------|
| `postgres-db` | `pg_isready` | `pg_isready -U postgres` | 10 s / 5 retries / 5 s |
| `springboot-app` | **⚠ PENDING** | None (Actuator not yet shipped) | — |
| `react-app` | None | — | — |

> **⚠ PENDING — Application-level healthcheck**: Spring Boot Actuator (`/actuator/health`) has not yet been shipped. Until it is, the `springboot-app` container has no health endpoint. Use the proxy-health approach in the [Current health probe](#current-health-probe) section below. When Actuator is available, update this document with the endpoints listed under [Actuator endpoints](#actuator-endpoints-pending).

### Check container health status

```bash
# Show running state and health status for all containers
docker compose ps

# Detailed health log for postgres-db
docker inspect postgres-db \
  --format '{{range .State.Health.Log}}{{.Start}} {{.ExitCode}} {{.Output}}{{"\n"}}{{end}}' \
  | tail -10
```

---

## Current health probe

Until Actuator ships, verify the backend is up by probing the public login endpoint:

```bash
curl -sf http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"probe@example.com","password":"noop"}' \
  -w "\nHTTP %{http_code}\n" \
  | grep -E "^HTTP (200|400|401)"
```

- **200/400/401** — backend is reachable and processing requests (401 means bad credentials, which is expected for a probe).
- **Connection refused / 502 / 503** — backend is not up. See [Triage: backend not responding](#triage-backend-not-responding).

---

## Actuator endpoints (PENDING)

> **⚠ PENDING**: These endpoints are not yet wired. Add them to the Spring Boot build (see ADR-0006 and the Actuator work order) before treating this section as executable.

When Actuator is available, the following management endpoints will be active:

| Endpoint | Purpose | Expected response |
|----------|---------|-------------------|
| `GET /actuator/health` | Aggregate liveness + readiness | `{"status":"UP"}` |
| `GET /actuator/health/liveness` | Liveness (restart the container if failing) | `{"status":"UP"}` |
| `GET /actuator/health/readiness` | Readiness (stop sending traffic if failing) | `{"status":"UP"}` |
| `GET /actuator/info` | Build info, git SHA, version | JSON with `build.*` and `git.*` |
| `GET /actuator/metrics` | JVM, HTTP, DB metrics | Prometheus-compatible JSON |

Update `docker-compose.yml` to use the healthcheck below once Actuator is deployed:

```yaml
# Future healthcheck for springboot-app (add to docker-compose.yml when Actuator is available)
healthcheck:
  test: ["CMD", "curl", "-sf", "http://localhost:8080/actuator/health/readiness"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s
```

---

## Log access and filtering

### Tail recent logs

```bash
# Tail the backend
docker compose logs -f springboot-app

# Tail all services simultaneously
docker compose logs -f

# Show only ERROR and WARN
docker compose logs springboot-app | grep -E "ERROR|WARN"
```

### Filter by correlation ID

Each error response body carries a `correlationId` field (see [authorization-regression.md](authorization-regression.md#structured-error-body-reference)). Use it as a join key between an alert and the corresponding log line:

```bash
# Replace <correlation-id> with the value from the alert or error body
docker compose logs springboot-app | grep "<correlation-id>"
```

If no `correlationId` is present (e.g., the error occurred before the exception handler ran), correlate by timestamp, container ID, and HTTP path:

```bash
# Look for requests around the timestamp of the alert (adjust the grep pattern)
docker compose logs springboot-app | grep "2026-08-10T14:32"
```

---

## Triage: backend not responding

```bash
# 1. Check container state
docker compose ps springboot-app

# 2. Check for recent restarts
docker inspect springboot-app --format '{{.RestartCount}} restarts'

# 3. Read the last 200 lines of the backend log
docker compose logs --tail=200 springboot-app

# 4. Look for OOM, port conflict, Flyway failure, or startup exception
docker compose logs springboot-app | grep -E "ERROR|FATAL|OOM|BindException|FlywayException|ContextRefreshFailed"
```

**Common causes**:

| Symptom in log | Cause | Remediation |
|----------------|-------|-------------|
| `FlywayException` or `Migration ... failed` | Flyway startup failure | See [migration-failure.md](migration-failure.md) |
| `Unable to acquire JDBC Connection` | `postgres-db` not healthy | Check `docker compose ps postgres-db`; wait for pg_isready healthcheck to pass |
| `Address already in use :8080` | Port conflict with another process | Stop the conflicting process; restart `springboot-app` |
| `ContextRefreshFailed` / `BeanCreationException` | Application context failed to load | Read the full stack trace; most common cause is missing required property |

---

## Triage: database not responding

```bash
# 1. Check postgres-db health status
docker compose ps postgres-db

# 2. Test pg_isready directly
docker compose exec postgres-db \
  pg_isready -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-fsmdb}"

# 3. If pg_isready passes, test with psql
docker compose exec postgres-db \
  psql -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-fsmdb}" -c "SELECT 1;"
```

If the database volume is corrupt or the container fails to start after repeated restarts, follow [database-backup-restore.md](database-backup-restore.md).

---

## Alert-to-log-line tracing

1. Receive alert with `correlationId` (or timestamp + endpoint).
2. `docker compose logs springboot-app | grep "<correlationId>"` to find the log line.
3. Note the Java class and line number in the stack trace.
4. Cross-reference with the relevant triage section:
   - 401/403 → [authorization-regression.md](authorization-regression.md)
   - Flyway error → [migration-failure.md](migration-failure.md)
   - Data-loss / corrupt state → [database-backup-restore.md](database-backup-restore.md)
   - Service down → this document, sections above.

---

## SLO reference

See [../operations/slo.md](../operations/slo.md) for:
- API availability SLO (target: 99.5%)
- Login success rate SLO (target: 99%)
- p95 latency SLO (target: < 400 ms)
- Migration success SLO (target: 100%)

Alert conditions and error budget policies are defined there.

---

## Verification

**Pass criterion**: `docker compose ps` shows all containers running (or healthy for `postgres-db`), the current health probe returns HTTP 200/400/401, and no ERROR-level log entries appear in the last 5 minutes.

---

## Escalation

| Condition | Escalate to | Maximum time |
|-----------|-------------|-------------|
| Container restart loop (> 3 restarts in 10 min) | Backend / Platform engineer | 10 minutes |
| Database volume corrupt or unrecoverable | DevOps engineer + DBA | Immediate |
| Application down > 30 min (SLO breach imminent) | Backend engineer + DevOps | 15 minutes |
