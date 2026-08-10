# ADR-0003: Service-layer tenancy scoping on AppUser.customer

| Field  | Value      |
|--------|------------|
| Status | Accepted   |
| Date   | 2026-08-10 |

## Context

The system is multi-tenant: each `Customer` organisation has its own sites, work orders, and user accounts. There is currently no database-level or ORM-level tenant isolation — `WorkOrderService.getAllWorkOrders()` calls `workRepo.findAll()` with no predicate, returning every work order in the database regardless of the caller's tenant. The frontend compensates with client-side filtering: `Dashboard.jsx` and `CustomerRequests.jsx` both call the unfiltered endpoint and then filter the response in JavaScript using `user.customerId`.

The `AppUser` entity carries a `@ManyToOne` association to `Customer` (column `customer_id`), making the tenant identifier a first-class field on every user record. This is the natural anchor for server-side scoping.

## Decision

We will implement tenancy scoping at the service layer by threading the authenticated user's `AppUser.customer` into every repository query that returns tenant-owned resources. Queries will be parameterised on `customerId` rather than relying on the caller to filter the result in application memory or on the client. The frontend client-side filtering is a transitional workaround that will be removed once server-side scoping is in place.

Hibernate's `@TenantId` annotation and PostgreSQL row-level security are considered but deferred; the service-layer approach is chosen as the lowest-risk first step that does not require schema changes.

## Consequences

- **Positive**: eliminates full-table scans on tenant-owned tables for privileged callers; reduces data leakage risk.
- **Positive**: the `AppUser.customer` field is already populated and accessible via Spring Security's `Authentication` principal — no new infrastructure required.
- **Positive**: scoping logic is explicit and testable at the unit level without a database.
- **Negative**: developers must remember to apply scoping in every new service method; there is no framework-level enforcement.
- **Negative**: MANAGER-role users who legitimately need cross-tenant visibility require explicit bypass logic (e.g. a null-customer check or a separate admin-scoped query).
- **Ongoing discipline**: every new `findAll`-style repository call must be reviewed for whether tenant scoping applies. The ArchUnit inventory test can be extended to flag unscoped repository calls.
- **Client-side filtering removal**: `Dashboard.jsx` and `CustomerRequests.jsx` client-side `.filter` calls on `customerId` are a known technical debt item to be removed once service-layer scoping covers the same endpoints.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| Hibernate `@TenantId` (Spring Data JPA multi-tenancy) | Requires a `TenantIdentifierResolver` bean, a Hibernate interceptor, and consistent schema discipline. Significant setup cost with risk of incorrect wiring; deferred to a dedicated multi-tenancy epic. |
| Schema-per-tenant | Requires dynamic datasource routing, schema provisioning automation, and connection pool partitioning. Operationally complex and does not fit the current single-database deployment model. |
| PostgreSQL row-level security (RLS) | Strong enforcement, but requires DDL changes (POLICY and SECURITY DEFINER functions), a tenant-aware datasource, and cannot be tested at the service-unit level. Considered the correct long-term direction but deferred pending the RLS epic. |
| Client-side filtering only | Already the current state; unacceptable long-term because it transmits all tenant data to every client, violating least-privilege. |

## Evidence

```
keystone-backend/src/main/java/com/fsm/keystone/service/WorkOrderService.java:83-85
  — getAllWorkOrders() calls workRepo.findAll() with no predicate; no tenant filter applied.

keystone-backend/src/main/java/com/fsm/keystone/entity/AppUser.java:47-48
  — @ManyToOne @JoinColumn(name = "customer_id") private Customer customer;
    — tenant identifier is a first-class field on AppUser.

frontend/src/pages/Dashboard.jsx:49-60
  — const customerId = user.customerId;
    — client-side filter: w.customer?.id === customerId || w.customerId === customerId

frontend/src/pages/CustomerRequests.jsx:22-48
  — getCustomerId() reads user.customerId; filteredWorkOrders and filteredSites are
    produced by .filter() on the full unscoped API response.
```

## Related ADRs

- [ADR-0001](0001-default-deny-security-filter-chain.md) — the filter-chain layer that authenticates the principal from which `AppUser.customer` is read.
- [ADR-0002](0002-method-security-preauthorize-archunit-gate.md) — method-security annotations that express which roles may access cross-tenant data.
