# Observability Runbook — fsm-keystone

## Probe Semantics

| Endpoint | Auth required | Purpose |
|---|---|---|
| `GET /actuator/health` | No | Overall health; components only visible to authenticated callers |
| `GET /actuator/health/liveness` | No | JVM liveness (process is running and not deadlocked) |
| `GET /actuator/health/readiness` | No | Readiness to serve traffic; includes `db` indicator |
| `GET /actuator/prometheus` | Yes (Bearer JWT) | Prometheus scrape target |

### Liveness vs Readiness split

**Liveness** (`livenessState` only): JVM is alive.  
A failing liveness probe causes the orchestrator to **restart** the container.  
The database indicator is **excluded** so a transient PostgreSQL outage does not trigger a restart loop.

**Readiness** (`readinessState` + `db`): application can serve requests.  
A failing readiness probe causes the orchestrator to **stop routing traffic** to the container without restarting it.  
This is the correct response to a database outage — wait for Postgres to recover, then traffic resumes automatically.

### Health body visibility

`management.endpoint.health.show-details=when_authorized` — anonymous callers see only `{"status":"UP"}`.  
Authenticated callers see the full component map, including the Hikari pool and datasource status.  
This prevents connection URLs, driver versions, and pool state from leaking to unauthenticated clients.

---

## Scrape Configuration

Prometheus scrapes `/actuator/prometheus` every **15 seconds** (`scrape_interval: 15s`).

A sample `scrape_configs` entry is committed at  
`keystone-backend/src/test/resources/fixtures/metrics/prometheus-scrape-config.yaml`.

The endpoint is protected by `anyRequest().authenticated()`. Supply a long-lived service-account JWT  
in `authorization.credentials_file`, or bind the management port to an internal interface:

```properties
management.server.port=9090  # not exposed in docker-compose.yml by default
```

---

## Key Metrics

| Metric | Description | Alert threshold |
|---|---|---|
| `http_server_requests_seconds` | Per-endpoint latency histogram (p50/p95/p99) | p95 > 400 ms for GET /api/work-orders |
| `hikaricp_connections_active` | Active connections in the pool | > 18 (max-pool-size 20) for 5 min |
| `hikaricp_connections_pending` | Threads waiting for a connection | > 0 for 1 min |
| `jvm_memory_used_bytes` | JVM heap used | > 80% of container memory limit |
| `process_cpu_usage` | CPU ratio 0–1 | > 0.8 for 5 min |
| `fsm_api_errors_total` | Domain errors by `code` and `status` | rate > 1/s for any 4xx code |

---

## SLO Baselines

| Endpoint | p95 target | Error rate target |
|---|---|---|
| `GET /api/work-orders` | ≤ 400 ms | < 0.1% 5xx |
| `GET /api/work-orders/{id}` | ≤ 200 ms | < 0.1% 5xx |
| `POST /api/auth/login` | ≤ 300 ms | < 0.5% 5xx |
| `POST /api/work-orders` | ≤ 500 ms | < 0.1% 5xx |

These are initial baselines derived from expected database query patterns. Adjust after load testing.

---

## Example Prometheus Alert Expressions

### Error rate spike
```promql
# Alert when any error code fires more than 1 req/s over a 5-minute window
rate(fsm_api_errors_total[5m]) > 1
```

### Pool starvation
```promql
# Alert when threads are waiting for a connection
hikaricp_connections_pending{pool="HikariPool-1"} > 0
```

### p95 latency breach
```promql
# Alert when 95th-percentile latency exceeds 400 ms
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{uri="/api/work-orders",method="GET"}[5m])
) > 0.4
```

### Connection pool nearly exhausted
```promql
hikaricp_connections_active{pool="HikariPool-1"} / 20 > 0.9
```

---

## HikariCP Sizing Rationale

| Property | Value | Reason |
|---|---|---|
| `maximum-pool-size` | 20 | Two replicas × 20 = 40 connections; well within PostgreSQL default `max_connections=100` |
| `minimum-idle` | 5 | Keep 5 connections warm to handle burst traffic without cold-start latency |
| `connection-timeout` | 3000 ms | Surface pool exhaustion as a fast typed 5xx via GlobalExceptionHandler rather than an unbounded wait |
| `max-lifetime` | 1800000 ms | Recycle connections every 30 min to avoid hitting PostgreSQL's `idle_in_transaction_session_timeout` |

All properties are environment-overridable via `HIKARI_MAX_POOL_SIZE`, `HIKARI_MIN_IDLE`,  
`HIKARI_CONNECTION_TIMEOUT`, `HIKARI_MAX_LIFETIME`. No secrets involved.

---

## Compose Healthcheck Rollback

If the `wget`-based healthcheck prevents the stack from starting (e.g., `wget` is absent in a future base image change), disable it temporarily:

```yaml
# In docker-compose.yml backend service:
healthcheck:
  disable: true
```

Then update the frontend `depends_on` back to:
```yaml
depends_on:
  - backend
```

For the long-term fix, either:
1. Switch the base image to one that includes `curl` and update the healthcheck command
2. Or expose the management port on a separate interface and use a Java-based probe

Document the decision in the image baseline (`docs/baseline/image-baseline.md`).
