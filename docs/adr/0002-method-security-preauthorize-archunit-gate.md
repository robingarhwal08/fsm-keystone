# ADR-0002: Method security via @PreAuthorize with ArchUnit enforcement

| Field  | Value      |
|--------|------------|
| Status | Accepted   |
| Date   | 2026-08-10 |

## Context

The filter-chain default-deny posture (ADR-0001) guards the application at the HTTP routing level, but it cannot express role distinctions within a single route group. A Manager and a Customer both reach `/api/work-orders`, but only a Manager or Dispatcher should be able to assign a technician. Spring Security's method-security layer (`@PreAuthorize`) provides this per-endpoint distinction.

The current state is partial: `WorkOrderController.create` carries `@PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','CUSTOMER')")` and `WorkOrderController.assign` carries `@PreAuthorize("hasAnyRole('MANAGER','DISPATCHER')")`, but `WorkOrderController.all` and `WorkOrderController.one` have no annotation at all. `CustomerController.delete` has its `@PreAuthorize` commented out. Without an automated gate, this inconsistency will grow as new endpoints are added.

The `EndpointAuthorizationInventoryTest` (WO-006) already scans every handler method via ArchUnit's `ClassFileImporter`, records the `@PreAuthorize` expression or `NONE`, and writes a deterministic inventory report. The count assertion in that test (`EXPECTED_ENDPOINT_COUNT = 34`) acts as a regression guard: any new endpoint that silently drops off the inventory fails the build.

## Decision

We will use `@EnableMethodSecurity` (enabled on `SecurityConfig`) together with `@PreAuthorize` on every handler method as the authoritative role-enforcement mechanism. The ArchUnit-driven `EndpointAuthorizationInventoryTest` is the automated gate: it reports the authorization status of every discovered endpoint and will be upgraded (in subsequent work orders) to fail the build when any handler method lacks an annotation.

`@EnableMethodSecurity` replaces the deprecated `@EnableGlobalMethodSecurity(prePostEnabled=true)` and is the Spring Security 6+ idiomatic form.

## Consequences

- **Positive**: role requirements are co-located with the handler; a reviewer can see the authorization policy without navigating to a central configuration file.
- **Positive**: the ArchUnit inventory provides a machine-readable audit trail of every endpoint's authorization state at every commit.
- **Positive**: `@EnableMethodSecurity` supports SpEL expressions, enabling complex policies (e.g. `hasRole('MANAGER') or (hasRole('CUSTOMER') and #req.customerId == principal.id)`) as requirements mature.
- **Negative**: incomplete annotation coverage (the current `NONE` entries) means some endpoints are protected only by the filter-chain `anyRequest().authenticated()` rule, not by role policy. This is a known gap tracked in the authorization epic.
- **Ongoing discipline**: every new handler method must carry `@PreAuthorize`; the ArchUnit gate will enforce this once the fail-on-missing rule is activated.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| Centralized `HttpSecurity.authorizeHttpRequests` rules only | Cannot express per-method role distinctions within a shared route prefix; any change requires editing the central config, increasing merge-conflict risk. |
| Spring Security ACL | Correct for domain-object ownership checks, but heavyweight to set up (requires ACL schema, `AclService`, `ObjectIdentity` tables). Deferred to a later phase when full multi-tenant RBAC is designed. |
| Custom `HandlerInterceptor` with reflection | Bypasses the standard Spring Security audit and test infrastructure; harder to test and impossible to lint with ArchUnit. |

## Evidence

```
keystone-backend/src/main/java/com/fsm/keystone/security/SecurityConfig.java:23
  — @EnableMethodSecurity annotation.

keystone-backend/src/main/java/com/fsm/keystone/controller/WorkOrderController.java:24,38
  — @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','CUSTOMER')") on create;
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER')") on assign.

keystone-backend/src/main/java/com/fsm/keystone/controller/WorkOrderController.java:31,35
  — all() and one() have no @PreAuthorize annotation (known gap).

keystone-backend/src/main/java/com/fsm/keystone/controller/CustomerController.java:47
  — @PreAuthorize("hasRole('MANAGER')") commented out on delete() (known gap).

keystone-backend/src/test/java/com/fsm/keystone/arch/EndpointAuthorizationInventoryTest.java
  — ArchUnit ClassFileImporter scans com.fsm.keystone.controller, records @PreAuthorize
    or NONE for every handler method, asserts EXPECTED_ENDPOINT_COUNT = 34.
```

## Related ADRs

- [ADR-0001](0001-default-deny-security-filter-chain.md) — the filter-chain layer that this method-security layer supplements.
