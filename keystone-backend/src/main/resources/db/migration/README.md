# db/migration

Flyway versioned SQL migration scripts live in this directory.

## Naming convention

```
V<version>__<snake_case_description>.sql
```

Examples:
- `V1__baseline_schema.sql` — initial DDL snapshot (WO-041)
- `V2__add_work_order_priority_column.sql`

Rules:
- Version numbers must be monotonically increasing integers or decimals (e.g. `1`, `2`, `2.1`).
- Use double underscore `__` to separate version from description.
- Description must use `snake_case` (no spaces, no hyphens).
- File names are case-sensitive on Linux; use lowercase consistently.

## Immutability policy

**Never modify or delete an applied migration.**
Once a script has been applied to any non-dev environment, it is immutable.
Flyway validates checksums on startup (`validate-on-migrate=true`); a checksum mismatch
causes a fail-fast startup error. Fix forward with a new migration; never patch old ones.

## Baseline

The first migration (`V1__baseline_schema.sql`) is a DDL snapshot of the 8 tables that
existed before Flyway was introduced (WO-041). Databases that predate migration tooling
are baselined using `spring.flyway.baseline-on-migrate=true` (dev profile) or via a
manual `flyway baseline` command before the first deploy to production.

## Environment configuration

| Profile | `baseline-on-migrate` | Meaning |
|---------|----------------------|---------|
| dev     | `true`               | Auto-baseline legacy databases for local development |
| prod    | `false`              | Require baseline migration to be applied explicitly  |

`clean-disabled=true` is enforced in all profiles; Flyway can never drop the schema.
