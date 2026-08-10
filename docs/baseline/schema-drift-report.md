# Schema Drift Report — fsm-keystone

**Baseline:** `docs/baseline/schema-snapshot.sql` (generated from JPA model; see `SchemaSnapshotTest`)
**Reference dump:** not obtained — see §Production Dump Status below
**Date:** 2026-08-10

---

## Production Dump Status

A schema-only production dump could not be obtained during this phase because it requires
operational access to the deployed database. The comparison in this report is therefore
**baseline-only** (generated snapshot vs. known entity anomalies).

To obtain the production dump and complete a full drift comparison, follow the runbook:
`docs/runbooks/schema-snapshot.md` — section "Capture a production schema dump".

Once the dump is available, re-run the diff command documented there and update the
per-table findings below with any columns present in production but absent from the
generated snapshot.

---

## Generated Snapshot vs. JPA Model — Summary

The snapshot in `docs/baseline/schema-snapshot.sql` was produced by the `SchemaSnapshotTest`
against an empty PostgreSQL 16 Testcontainers instance. It represents the schema the
**current JPA model would create from scratch** — not necessarily what the deployed database
looks like after a history of `ddl-auto=update` boots.

| Table | Columns in snapshot | Known anomalies |
|---|---|---|
| `customers` | 7 | None detected |
| `part_usage` | 8 | None detected |
| `parts` | 7 | Missing `reorder_level`, `supplier` (Finding A) |
| `sites` | 11 | 8 PII fields (Finding D) |
| `status_history` | 7 | None detected |
| `time_logs` | 7 | None detected |
| `users` | 9 | None detected |
| `work_orders` | 16 | `actual_start`/`actual_end` undocumented (Finding B); collision-prone WO number (Finding C) |

---

## Per-Table Drift Analysis

### `customers`
- **Snapshot columns:** `id`, `name`, `email`, `phone`, `billing_address`, `created_at`, `updated_at`
- **Production columns:** unknown (dump not obtained)
- **Known drift risk:** low — entity is stable

### `parts`
- **Snapshot columns:** `id`, `part_name`, `part_number`, `description`, `unit_price`, `stock_quantity`, `active`
- **Production columns:** unknown (dump not obtained)
- **Known drift risk:** if a `reorder_level` or `supplier` column was added manually to production, it will appear in the dump but not the snapshot — this is an expected orphan column

### `sites`
- **Snapshot columns:** `id`, `site_name`, `address`, `city`, `state`, `pincode`, `latitude`, `longitude`, `contact_person`, `contact_phone`, `customer_id`
- **Production columns:** unknown (dump not obtained)
- **Known drift risk:** high — Site has 8 PII fields added at some point; a production database started from an earlier entity shape may be missing columns if `ddl-auto=update` missed an add

### `work_orders`
- **Snapshot columns:** `id`, `work_order_number`, `title`, `description`, `status`, `priority`, `scheduled_start`, `scheduled_end`, `actual_start`, `actual_end`, `created_at`, `updated_at`, `customer_id`, `site_id`, `created_by_user_id`, `assigned_technician_id`
- **Production columns:** unknown (dump not obtained)
- **Known drift risk:** medium — `actual_start` and `actual_end` were added without a corresponding DTO; if added while production was running with `update`, they should be present; if an earlier schema pre-dates these fields, they may be absent

---

## Type Mismatch Risks

These type choices in the JPA model have migration implications:

| Column | JPA type | PostgreSQL type | Risk |
|---|---|---|---|
| `work_orders.status`, `work_orders.priority`, `status_history.old_status`, `status_history.new_status`, `users.role` | `@Enumerated(STRING)` | `VARCHAR(255)` | No type-check constraint; invalid string values pass silently |
| `part_usage.total_cost`, `time_logs.hours_spent` | `BigDecimal` | `NUMERIC(38, 2)` | Scale of 2 limits sub-cent precision; may need `NUMERIC(38, 4)` for currencies |
| `sites.latitude`, `sites.longitude` | `Double` | `FLOAT(53)` | Floating-point arithmetic; consider `NUMERIC(9, 6)` for geographic coordinates |

---

## Missing Indexes

The generated snapshot contains no explicit indexes beyond PRIMARY KEY and UNIQUE constraints.
The following foreign key columns are unindexed and will cause sequential scans on common joins:

| Table | Column | Used in join |
|---|---|---|
| `users` | `customer_id` | Fetch user's customer |
| `sites` | `customer_id` | List sites for customer |
| `work_orders` | `customer_id` | List work orders for customer |
| `work_orders` | `site_id` | List work orders for site |
| `work_orders` | `assigned_technician_id` | List work orders for technician |
| `work_orders` | `created_by_user_id` | Audit trail |
| `part_usage` | `work_order_id` | Parts for a work order |
| `part_usage` | `part_id` | Usage history for a part |
| `time_logs` | `work_order_id` | Time logs for a work order |
| `time_logs` | `technician_id` | Time logs for a technician |
| `status_history` | `work_order_id` | Status history for a work order |

These should be added in the V1 Flyway baseline migration.

---

## Follow-up Actions (Flyway Epic)

| Action | Owner |
|---|---|
| Capture production schema dump and complete per-table drift column diff | DBA / ops |
| Create V1 Flyway baseline from reconciled snapshot + production dump | Backend team |
| Set `spring.flyway.baseline-version=1` and flip `ddl-auto=validate` | Backend team |
| Add `reorder_level INTEGER` column to `parts` (Finding A) | Backend team |
| Resolve `actual_start`/`actual_end` — expose via API or drop columns (Finding B) | Backend team |
| Replace timestamp WO number generator with a DB sequence (Finding C) | Backend team |
| Add indexes for all FK columns listed above | DBA / backend |
| Add CHECK constraints for enum columns (`status`, `priority`, `role`) | DBA / backend |
| Implement DTO masking for Site PII fields (Finding D) | Backend team |
