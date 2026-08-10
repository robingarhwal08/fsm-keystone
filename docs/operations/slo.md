# Service Level Objectives — FSM Keystone

| Field | Value |
|-------|-------|
| Owner | DevOps engineer + Backend engineer |
| Reviewed | 2026-08-10 |
| Review cadence | Quarterly (or after any SLO breach) |

---

## Overview

This document defines the service-level indicators (SLIs) and service-level objectives (SLOs) for the FSM Keystone platform. SLOs are internal targets; no external SLA is in effect at this stage.

Error budgets are calculated over a 30-day rolling window.

---

## SLI and SLO table

| # | SLI name | Measurement | Target SLO | Measurement window |
|---|----------|-------------|------------|--------------------|
| 1 | **API availability** | Ratio of HTTP responses with status 2xx or 4xx (expected errors) to total requests | **99.5%** | 30-day rolling |
| 2 | **Login success rate** | Ratio of successful `/api/auth/login` responses (HTTP 200) to total login attempts (excluding obvious bot traffic) | **99%** | 30-day rolling |
| 3 | **p95 API latency** | 95th-percentile HTTP response time for all `/api/**` endpoints | **< 400 ms** | 30-day rolling |
| 4 | **Migration success** | Ratio of deployments where all Flyway migrations complete with `success = true` before application start | **100%** | Per-deployment |

---

## SLI definitions

### SLI-1: API availability

- **Good events**: HTTP responses with status code 1xx, 2xx, 3xx, or 4xx (client errors are expected; they are not availability failures).
- **Bad events**: HTTP 5xx responses, connection timeouts, and requests that fail before reaching the application (e.g., container crash, port unreachable).
- **Measurement source**: application access log (`springboot-app`). When Actuator `/actuator/metrics` is available, use `http.server.requests` metric filtered by `status!=5xx`.

> **⚠ PENDING**: Actuator `/actuator/metrics` endpoint not yet shipped. Until then, derive availability from log-based 5xx counting.

### SLI-2: Login success rate

- **Good events**: POST `/api/auth/login` returns HTTP 200.
- **Bad events**: POST `/api/auth/login` returns HTTP 5xx, connection timeout, or no response.
- **Excluded**: HTTP 401 (bad credentials) and 400 (malformed request) — these are client errors, not service failures.
- **Measurement source**: application access log filtered to `POST /api/auth/login`.

### SLI-3: p95 API latency

- **Measurement**: 95th percentile of HTTP response time in milliseconds across all `/api/**` endpoints.
- **Excluded**: health-probe requests and requests that return 5xx (already counted in availability).
- **Measurement source**: application access log response-time field. When Actuator ships, use `http.server.requests` histogram metric `p95`.

> **⚠ PENDING**: Actuator `/actuator/metrics` endpoint not yet shipped.

### SLI-4: Migration success

- **Good events**: a deployment where `SELECT COUNT(*) FROM flyway_schema_history WHERE success = false` returns 0 after application start.
- **Bad events**: a deployment where any row in `flyway_schema_history` shows `success = false`, or the application fails to start due to a Flyway exception.
- **Measurement source**: `flyway_schema_history` table inspection (see [migration-failure.md](../runbooks/migration-failure.md)) and deployment logs.

---

## Error budget policy

| SLI | Monthly error budget (at target SLO) | Budget-burn alert (fast-burn) | Exhaustion action |
|-----|--------------------------------------|-------------------------------|-------------------|
| API availability 99.5% | 0.5% of monthly requests ≈ 216 min downtime | Burn rate > 14.4× in 1 h window | Freeze non-critical deploys; escalate to on-call |
| Login success rate 99% | 1% of login attempts | Burn rate > 14.4× in 1 h window | Investigate authentication service immediately |
| p95 latency < 400 ms | 5% of requests may exceed 400 ms | p99 > 1 s sustained for 10 min | Profile and rollback last deploy if correlated |
| Migration success 100% | Zero failures permitted | Any failed migration row | Block all subsequent deploys; activate migration-failure runbook |

**Deploy freeze trigger**: if the combined API availability + login success error budgets are more than 50% consumed in the last 7 days, defer non-critical releases and schedule a reliability review.

---

## Alert conditions

| Alert | Condition | Severity | Runbook |
|-------|-----------|----------|---------|
| High 5xx rate | HTTP 5xx rate > 1% over 5-minute window | Critical | [health-and-observability.md](../runbooks/health-and-observability.md) |
| Login failure spike | Login 5xx rate > 2% over 5-minute window | Critical | [authorization-regression.md](../runbooks/authorization-regression.md) |
| Latency breach | p95 response time > 400 ms for 10 minutes | Warning | [health-and-observability.md](../runbooks/health-and-observability.md) |
| Latency severe | p95 response time > 1 s for 5 minutes | Critical | [deploy-and-rollback.md](../runbooks/deploy-and-rollback.md) |
| Migration failure | Any row in `flyway_schema_history` with `success = false` | Critical | [migration-failure.md](../runbooks/migration-failure.md) |
| Database unhealthy | `postgres-db` container healthcheck fails 3 times | Critical | [health-and-observability.md](../runbooks/health-and-observability.md) + [database-backup-restore.md](../runbooks/database-backup-restore.md) |
| Container restart loop | Any container restarts > 3 times in 10 minutes | Critical | [health-and-observability.md](../runbooks/health-and-observability.md) |

> **⚠ PENDING**: Automated alerting is not yet wired. Until a monitoring stack (e.g., Prometheus + Alertmanager) is deployed, these conditions must be checked manually or via log-based alerting.

---

## SLO review cadence

SLOs are reviewed:
- **Quarterly** as part of the reliability review.
- **After any SLO breach** (SLO breach = missing the target in the measurement window).
- **After any significant architecture change** (e.g., adding Actuator, introducing caching, sharding the database).

Review actions:
1. Check the actual error rate vs. SLO target for each SLI.
2. Identify breach root causes from the incident log.
3. Adjust targets if they are consistently met with wide margin (consider tightening) or frequently breached (consider process changes before loosening).
4. Update this document with the new targets and the rationale.
