# Coupling Metrics Baseline — WorkOrderService

**Baseline date**: 2026-08-10  
**Source files**: `keystone-backend/src/main/java/com/fsm/keystone/service/WorkOrderService.java`  
**Related**: [layer-violation-triage.md](layer-violation-triage.md) | [exception-debt.md](exception-debt.md)

---

## WorkOrderService fan-out: eight injected repositories

`WorkOrderService` is the highest-coupling class in the codebase. All eight Spring Data JPA repositories are injected via `@RequiredArgsConstructor`:

| Field | Repository | Owner aggregate | Used by methods |
|---|---|---|---|
| `workRepo` | `WorkOrderRepository` | WorkOrder | createWorkOrder, getAllWorkOrders, getWorkOrderById, assignTechnician, updateStatus, addPartUsage, addTimeLog, updateWorkOrder, deleteWorkOrder |
| `customerRepo` | `CustomerRepository` | Customer | createWorkOrder, updateWorkOrder |
| `siteRepo` | `SiteRepository` | Site | createWorkOrder, updateWorkOrder |
| `userRepo` | `UserRepository` | AppUser | createWorkOrder, assignTechnician, updateStatus, addPartUsage, addTimeLog, updateWorkOrder |
| `historyRepo` | `StatusHistoryRepository` | StatusHistory | assignTechnician, updateStatus, getWorkOrderHistory |
| `partRepo` | `PartRepository` | Part | addPartUsage |
| `partUsageRepo` | `PartUsageRepository` | PartUsage | addPartUsage |
| `timeLogRepo` | `TimeLogRepository` | TimeLog | addTimeLog |

**Source**: `WorkOrderService.java:37-44`

---

## WorkOrderService fan-in: three calling controllers

Three `@RestController` beans inject and call `WorkOrderService`:

| Controller | Methods delegated | Source line |
|---|---|---|
| `WorkOrderController` | `createWorkOrder`, `getAllWorkOrders`, `getWorkOrderById`, `assignTechnician`, `updateStatus`, `getWorkOrderHistory`, `addPartUsage`, `addTimeLog`, `updateWorkOrder`, `deleteWorkOrder` | `WorkOrderController.java:1-99` |
| `TimeLogController` | `addTimeLog` (via `createTimeLog` handler) | `TimeLogController.java:23-26` |
| `PartUsageController` | `addPartUsage` (via `create` handler) | `PartUsageController.java:20-27` |

`TimeLogController` and `PartUsageController` are façade controllers: they delegate directly to `WorkOrderService` and add no domain logic of their own. This means `WorkOrderService.addTimeLog` and `WorkOrderService.addPartUsage` are each callable from two call sites (the façade controller AND `WorkOrderController.addTime`/`WorkOrderController.addPart`).

---

## Per-method side-effect table

### `assignTechnician(Long id, AssignTechnicianRequest req)`

**Source**: `WorkOrderService.java:95-123`

| Side effect | Detail | Source line | Risk note |
|---|---|---|---|
| Sets `WorkOrder.status` to `ASSIGNED` | Implicit status transition — not guarded by a state machine | `:108` | Any caller can force ASSIGNED regardless of current status |
| Inserts `StatusHistory` row | `changedBy` is set to `savedWorkOrder.getAssignedTechnician()` | `:116` | **Known correctness bug**: `changedBy` should be the *acting user* (the manager or dispatcher performing the assignment), not the technician being assigned. This field is currently client-supplied via `AssignTechnicianRequest.technicianId` rather than derived from `SecurityContextHolder`. Fix is in the actor-derivation work order (Phase 2). |
| Two `save()` calls | `workRepo.save(workOrder)` then `historyRepo.save(history)` — two separate flushes | `:110`, `:120` | No `@Transactional`: if `historyRepo.save` fails, `workRepo.save` is already committed. |

**Missing `@Transactional`**: Both writes are performed under Spring's implicit per-operation transaction default. A failure between the two saves will leave `WorkOrder.status = ASSIGNED` with no corresponding `StatusHistory` row. This violates the audit invariant.

**Missing `@Version` on `Part`**: Not directly relevant to this method, but noted here as context for the structural gap.

---

### `updateStatus(Long id, StatusUpdateRequest req)`

**Source**: `WorkOrderService.java:125-154`

| Side effect | Detail | Source line | Risk note |
|---|---|---|---|
| Updates `WorkOrder.status` | New status from `req.status()` record accessor | `:133` | No state-machine guard; any status transition is permitted |
| Inserts `StatusHistory` row | `changedBy` resolved from `req.changedByUserId()` (client-supplied) | `:137-141`, `:143-150` | `changedBy` is client-controlled — attacker can attribute status changes to any user ID |
| Two `save()` calls | `workRepo.save()` then `historyRepo.save()` | `:135`, `:153` | Same two-flush non-transactional risk as `assignTechnician` |

**Note**: `req.status()` (the `StatusUpdateRequest` record accessor) is the call the static analysis tool misidentified as a reference to `WorkOrderController.status`. See edge 1 in [layer-violation-triage.md](layer-violation-triage.md).

---

### `addPartUsage(Long id, PartUsageRequest req)`

**Source**: `WorkOrderService.java:164-208`

| Side effect | Detail | Source line | Risk note |
|---|---|---|---|
| Decrements `Part.stockQuantity` | `part.setStockQuantity(part.getStockQuantity() - quantityUsed)` | `:179` | No floor check: quantity can go negative. No concurrency guard. |
| Saves `Part` (stock decrement) | `partRepo.save(part)` | `:183` | Flush 1 of 2 |
| Inserts `PartUsage` row with unit-price snapshot | `unitPriceAtUsage` captured at time of save | `:184-190`, `:198-207` | Price snapshot is correct by design (prevents retroactive cost changes) |
| Three separate saves / accesses | `workRepo.findById`, `partRepo.findById`, `partRepo.save`, `partUsageRepo.save` | `:166-207` | No `@Transactional`: stock decrement and part-usage insert are not atomic |

**Missing optimistic locking on `Part`**: `Part` entity has no `@Version` field. Concurrent `addPartUsage` calls on the same part will produce a lost-update race condition: two threads can both read `stockQuantity = 5`, both subtract 1, and both write `4` — resulting in stock being decremented by 1 instead of 2. The `@Version` annotation is the standard JPA guard for this.

**Missing `@Transactional`**: Stock decrement (`partRepo.save`) and part-usage insert (`partUsageRepo.save`) are separate transactions. A failure between them leaves stock decremented with no `PartUsage` audit record.

---

### `addTimeLog(Long id, TimeLogRequest req)`

**Source**: `WorkOrderService.java:210-243`

| Side effect | Detail | Source line | Risk note |
|---|---|---|---|
| Computes `hoursSpent` from `startTime`/`endTime` | `Duration.between(req.startTime(), req.endTime()).toMinutes() / 60.0` | `:224-228` | Server-side derivation is correct by design. However, `startTime` and `endTime` are still client-supplied in the current DTO (`TimeLogRequest.startTime`, `TimeLogRequest.endTime`), so duration is only as trustworthy as the client's timestamps. |
| Inserts `TimeLog` row | `timeLogRepo.save(timeLog)` | `:242` | Single flush — less risky than the two-flush methods |

**`technicianId` is client-supplied**: `TimeLogRequest.technicianId` is sent by the client (`TimeLogs.jsx` reads it from `localStorage`). An attacker can log time against any technician. Fix is in the actor-derivation work order (Phase 2).

---

## Decomposition seams (observed, not prescribed)

The codebase already has four natural decomposition seams that the WorkOrderService decomposition epic should follow:

| Seam | Responsibility | Methods | Future service name |
|---|---|---|---|
| **Lifecycle** | WorkOrder CRUD, status transitions, history | `createWorkOrder`, `getAllWorkOrders`, `getWorkOrderById`, `updateStatus`, `getWorkOrderHistory`, `updateWorkOrder`, `deleteWorkOrder` | `WorkOrderLifecycleService` |
| **Assignment** | Technician assignment | `assignTechnician` | `WorkOrderAssignmentService` |
| **Consumption** | Part usage and stock management | `addPartUsage` | `WorkOrderConsumptionService` |
| **Time** | Time logging and duration derivation | `addTimeLog` | `WorkOrderTimeService` (or merged into `WorkOrderConsumptionService`) |

These seams are documented here as observations from the coupling analysis. The decomposition epic owns the actual split.

---

## Summary of structural risks

| Risk | Severity | Fix-in epic |
|---|---|---|
| Missing `@Transactional` on two-save methods (`assignTechnician`, `updateStatus`, `addPartUsage`) | HIGH — audit trail can desync from entity state | Phase 2 (annotation sweep) |
| Missing `@Version` on `Part` (lost-update on concurrent stock decrement) | HIGH — inventory accuracy | Phase 5 (DTO/pagination epic) |
| `changedBy` in `assignTechnician` records the wrong actor | MEDIUM — audit correctness bug | Phase 2 (actor-derivation work order) |
| Client-supplied `changedByUserId` in `updateStatus` | HIGH — privilege escalation / audit tampering | Phase 2 (actor-derivation work order) |
| Client-supplied `technicianId` in `addTimeLog` | HIGH — labour attribution manipulation | Phase 2 (actor-derivation work order) |
| No state-machine guard on `updateStatus` | MEDIUM — arbitrary transitions permitted | Phase 7 (WorkOrderService decomposition) |
