# Layer Violation Triage — FSM Keystone

**Baseline date**: 2026-08-10  
**Triage method**: For each reported edge, the source class was opened, the fully qualified call target resolved, and the cross-package import list inspected. Verdict is based on evidence, not assumption.  
**ArchUnit confirmation**: `LayerRulesTest.serviceLayer_mustNotDependOnController_reportingOnly` reports **0 genuine violations** in production code (all 15 are false positives).  
**Related register**: [coupling-metrics.md](coupling-metrics.md) | [exception-debt.md](exception-debt.md)

---

## Evidence: zero cross-package imports

Before adjudicating individual edges, the definitive structural check was run:

```bash
grep -rn "import com.fsm.keystone.controller" \
  keystone-backend/src/main/java/com/fsm/keystone/service/
# → 0 results

grep -rn "import com.fsm.keystone.controller" \
  keystone-backend/src/main/java/com/fsm/keystone/config/
# → 0 results
```

No service or config class imports anything from `com.fsm.keystone.controller`. The 15 reported edges therefore cannot be genuine compile-time dependencies; they are call-graph extraction artifacts.

---

## Triage table (15 edges)

| # | Source class | Source method | Reported target | Verdict | Evidence (file : line) | Remediation owner |
|---|---|---|---|---|---|---|
| 1 | `WorkOrderService` | `updateStatus` | `status` (WorkOrderController) | **FALSE POSITIVE** — name collision | `WorkOrderService.java:133` — calls `req.status()` where `req` is `StatusUpdateRequest`; the `.status()` call is the record accessor method, not `WorkOrderController.status()`. No import of `WorkOrderController` in `WorkOrderService`. | N/A (no violation) |
| 2 | `AuthService` | `signup` | `signup` (AuthController) | **FALSE POSITIVE** — name collision | `AuthService.java:27` — `signup` is the service method itself. The extractor matched the service method name against the controller method of the same name. `AuthService` imports only `com.fsm.keystone.repository`, `com.fsm.keystone.security`, and `com.fsm.keystone.dto`. | N/A (no violation) |
| 3 | `AuthService` | `login` | `login` (AuthController) | **FALSE POSITIVE** — name collision | `AuthService.java:62` — `login` is the service method itself. Same name-collision pattern as edge 2. `AuthService` has no import of `AuthController`. | N/A (no violation) |
| 4 | `WorkOrderService` | `getAllWorkOrders` | `all` (WorkOrderController) | **FALSE POSITIVE** — name collision | `WorkOrderService.java:85` — calls `workRepo.findAll()`. The extractor matched `findAll` fragment "all" against `WorkOrderController.all()`. No import of any controller. | N/A (no violation) |
| 5 | `WorkOrderService` | `getWorkOrderHistory` | `all` (WorkOrderController) | **FALSE POSITIVE** — name collision | `WorkOrderService.java:158` — calls `historyRepo.findAll()`. Same "all" fragment match. | N/A (no violation) |
| 6 | `CustomerService` | `getAllCustomers` | `all` (CustomerController) | **FALSE POSITIVE** — name collision | `CustomerService.java:20-22` — calls `customerRepository.findAll()`. No import of `CustomerController`. | N/A (no violation) |
| 7 | `SiteService` | `getAllSites` | `all` (SiteController) | **FALSE POSITIVE** — name collision | `SiteService.java:35-37` — calls `siteRepository.findAll()`. No import of `SiteController`. | N/A (no violation) |
| 8 | `WorkOrderService` | `deleteWorkOrder` | `delete` (WorkOrderController) | **FALSE POSITIVE** — name collision | `WorkOrderService.java:292-294` — calls `workRepo.deleteById(id)`. The `deleteById` call fragment matches controller method names containing "delete". No import of any controller. | N/A (no violation) |
| 9 | `CustomerService` | `deleteCustomer` | `delete` (CustomerController) | **FALSE POSITIVE** — name collision | `CustomerService.java:42-44` — method named `deleteCustomer` internally calls `customerRepository.deleteById(id)`. No import of `CustomerController`. | N/A (no violation) |
| 10 | `SiteService` | `deleteSite` | `delete` (SiteController) | **FALSE POSITIVE** — name collision | `SiteService.java:90-97` — calls `siteRepository.delete(site)`. The `.delete()` call fragment matched `SiteController.delete`. No import of `SiteController`. | N/A (no violation) |
| 11 | `WorkOrderService` | `updateWorkOrder` | `update` (WorkOrderController) | **FALSE POSITIVE** — name collision | `WorkOrderService.java:245-289` — method named `updateWorkOrder` calls `workRepo.save(wo)`. No import of any controller. | N/A (no violation) |
| 12 | `WorkOrderService` | `createWorkOrder` | `create` (WorkOrderController) | **FALSE POSITIVE** — name collision | `WorkOrderService.java:46-81` — method named `createWorkOrder` calls `workRepo.save(workOrder)`. No import of any controller. | N/A (no violation) |
| 13 | `WorkOrderService` | `addPartUsage` | `create` (PartUsageController) | **FALSE POSITIVE** — name collision | `WorkOrderService.java:164-208` — calls `partUsageRepo.save(partUsage)`. No import of `PartUsageController`. `PartUsageController` delegates to this method (controller→service direction is correct). | N/A (no violation) |
| 14 | `WorkOrderService` | `addTimeLog` | `createTimeLog` (TimeLogController) | **FALSE POSITIVE** — name collision | `WorkOrderService.java:210-243` — calls `timeLogRepo.save(timeLog)`. No import of `TimeLogController`. Correct direction is `TimeLogController → WorkOrderService`. | N/A (no violation) |
| 15 | `ApplicationConfig` | `userDetailsService` | — | **FALSE POSITIVE** — method name fragment | `ApplicationConfig.java:20-25` — `userDetailsService()` lambda calls `userRepository.findByEmail(...).orElseThrow(...)`. No controller import. Config layer → repository is a permitted dependency. | N/A (no violation) |

---

## Annotation-coverage gaps (related finding)

The layer-violation analysis did not find genuine cross-package violations, but the annotation-coverage pass (`HANDLER_MUST_HAVE_PREAUTHORIZE` in `LayerRulesTest`) found **24 handler methods missing `@PreAuthorize`**. These are distinct from layer violations but represent the authorization-gap risk that the default-deny epic must close.

| Controller | Missing @PreAuthorize count | Notes |
|---|---|---|
| `AuthController` | 2 | `signup`, `login` — intentionally `permitAll`; will receive annotations in annotation-sweep epic |
| `CustomerController` | 2 | `all` (GET /), `delete` (DELETE /{id} — annotation commented out, ADR-0001 known gap) |
| `PartController` | 2 | `all` (GET /), `one` (GET /{id}) |
| `PartUsageController` | 1 | `create` (POST /) |
| `SiteController` | 5 | All five endpoints unannotated |
| `TimeLogController` | 1 | `createTimeLog` (POST /) |
| `UserController` | 4 | All four endpoints unannotated |
| `WorkOrderController` | 7 | `all`, `one`, `history`, `addPart`, `addTime`, `update`, `delete` |

**ArchUnit baseline**: `EXPECTED_ANNOTATION_COVERAGE_VIOLATIONS = 24` in `LayerRulesTest.java`.  
**Cross-check**: `EndpointAuthorizationInventoryTest.EXPECTED_ENDPOINT_COUNT = 34`; 10 carry `@PreAuthorize`, 24 do not. Counts are consistent.

---

## Remediation index

| Finding | Owning epic | Priority |
|---|---|---|
| All 15 layer-violation edges — false positives | None required | N/A — close ticket |
| 24 missing `@PreAuthorize` annotations | Annotation sweep (Phase 2) | P1 — prerequisite for default-deny cutover |
| `CustomerController.delete` annotation commented out | Authorization remediation (Phase 2) | P1 |
| `AuthController` intentional public routes | Reviewed in filter-chain hardening (Phase 2) | P2 |

> Once all 24 annotation gaps are remediated, promote `EXPECTED_ANNOTATION_COVERAGE_VIOLATIONS` to `0` in `LayerRulesTest` to convert the harness from reporting-only to a blocking gate.
