# ADR-0007: Request/response records with Bean Validation and Page envelopes

| Field  | Value      |
|--------|------------|
| Status | Accepted   |
| Date   | 2026-08-10 |

## Context

Several controllers currently bind JPA entity classes directly as `@RequestBody` or return them as response bodies. For example, `CustomerController.create` accepts `@RequestBody Customer customer` and returns a `Customer` entity. Binding JPA entities directly to HTTP bodies has well-known problems: Jackson serialises lazy-loaded associations (causing `N+1` queries or `LazyInitializationException`), `@OneToMany` collections in the request can be silently overwritten, and the response leaks internal fields (audit columns, association IDs) that should not be part of the API contract.

The `UserController` demonstrates the target pattern: it uses `UserResponse` (a Java `record`) as the response type and `UpdateUserRequest` (a `record` with `@NotBlank`, `@Email` Bean Validation annotations) as the request type. `UserResponse.fromEntity` maps the entity to the DTO without exposing internals. Request DTOs in `com.fsm.keystone.dto` are already Java records: `AuthRequest`, `CreateWorkOrderRequest`, `SignupRequest`, `TimeLogRequest`, `PartUsageRequest`, `StatusUpdateRequest`.

## Decision

We will standardise all controller request and response bodies on immutable Java records in `com.fsm.keystone.dto`. Request records carry Bean Validation constraints (`@NotBlank`, `@Email`, `@NotNull`, etc.) and are bound with `@Valid`. Response records use a `fromEntity` factory method to project only the fields the API contract requires. JPA entity classes are never used as `@RequestBody` parameters or as direct return types from public controller methods. Paginated list endpoints return a `Page<ResponseRecord>` envelope.

## Consequences

- **Positive**: Bean Validation failures produce structured 400 responses rather than 500s from null-pointer exceptions at the persistence layer.
- **Positive**: response records decouple the API contract from the database schema; renaming a column does not break the API contract.
- **Positive**: Java records are immutable and concise; no boilerplate setters or `@Builder` needed.
- **Negative**: every new endpoint requires a matching DTO pair; adds a small amount of ceremony per feature.
- **Negative**: existing controllers (`CustomerController`, `WorkOrderController`) currently bind entities directly; migrating them is a multi-story effort tracked in the controller hardening epic.
- **Ongoing discipline**: code review must reject any controller method that accepts or returns a JPA entity directly.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| Bind JPA entities directly (current state for some controllers) | Leaks internal fields, causes lazy-load serialisation errors, and tightly couples API contract to database schema. |
| MapStruct code generation | Powerful for large entity graphs but adds a build-time annotation processor dependency. Java record `fromEntity` factories achieve the same result with less machinery. |
| Lombok `@Data` DTOs (mutable) | Mutable DTOs can be accidentally modified between validation and persistence. Java records are immutable by construction. |
| GraphQL or OpenAPI-generated DTOs | Over-engineered for a REST API without a published schema contract. REST + records is the pragmatic choice for the programme timeline. |

## Evidence

```
keystone-backend/src/main/java/com/fsm/keystone/dto/UserResponse.java
  — public record UserResponse(...) with static UserResponse fromEntity(AppUser user).

keystone-backend/src/main/java/com/fsm/keystone/dto/SignupRequest.java
  — public record SignupRequest(@NotBlank ... @Email String email, ...).

keystone-backend/src/main/java/com/fsm/keystone/controller/UserController.java:22,28,36
  — getAllUsers() returns ResponseEntity<List<UserResponse>>;
    updateUser() accepts @Valid @RequestBody UpdateUserRequest.

keystone-backend/src/main/java/com/fsm/keystone/controller/CustomerController.java:20
  — create(@RequestBody Customer customer) — current gap: entity bound directly.
    Target: accept CreateCustomerRequest record, return CustomerResponse record.
```

## Related ADRs

- [ADR-0002](0002-method-security-preauthorize-archunit-gate.md) — method-security expressions may reference DTO fields (e.g. `#req.customerId`) once entity binding is replaced.
- [ADR-0003](0003-service-layer-tenancy-scoping.md) — response DTOs should not expose cross-tenant identifiers; the `fromEntity` projection controls this.
