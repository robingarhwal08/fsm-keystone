package com.fsm.keystone.fixtures;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.entity.Site;
import com.fsm.keystone.entity.WorkOrder;
import com.fsm.keystone.enums.Priority;
import com.fsm.keystone.enums.WorkOrderStatus;

/**
 * Object-mother factory for {@link WorkOrder} entities.
 *
 * <p>Minimum valid shape: {@code workOrderNumber}, {@code title}, {@code customer},
 * and {@code site} are required. {@code status} and {@code priority} default in
 * {@code @PrePersist} but are set explicitly here for determinism.</p>
 *
 * <p>Work orders in the CREATED state have no assigned technician, matching
 * the dispatcher screen scenario where unassigned jobs are listed.</p>
 */
public final class WorkOrderFixtures {

    private WorkOrderFixtures() {}

    /** Returns a builder for a CREATED work order (no technician assigned). */
    public static WorkOrder.WorkOrderBuilder aCreatedWorkOrder(Customer customer, Site site, AppUser createdBy) {
        return WorkOrder.builder()
                .workOrderNumber("WO-TEST-" + System.currentTimeMillis())
                .title("Test Work Order")
                .description("Generic test work order")
                .status(WorkOrderStatus.CREATED)
                .priority(Priority.MEDIUM)
                .scheduledStart(FixtureClock.NOW)
                .scheduledEnd(FixtureClock.PLUS_EIGHT_HOURS)
                .createdAt(FixtureClock.NOW)
                .updatedAt(FixtureClock.NOW)
                .customer(customer)
                .site(site)
                .createdBy(createdBy)
                .assignedTechnician(null);
    }

    /** Returns a builder for an ASSIGNED work order with a specific technician. */
    public static WorkOrder.WorkOrderBuilder anAssignedWorkOrder(Customer customer, Site site,
                                                                  AppUser createdBy, AppUser technician) {
        return aCreatedWorkOrder(customer, site, createdBy)
                .status(WorkOrderStatus.ASSIGNED)
                .assignedTechnician(technician);
    }

    /** Returns a builder for an IN_PROGRESS work order. */
    public static WorkOrder.WorkOrderBuilder anInProgressWorkOrder(Customer customer, Site site,
                                                                    AppUser createdBy, AppUser technician) {
        return anAssignedWorkOrder(customer, site, createdBy, technician)
                .status(WorkOrderStatus.IN_PROGRESS)
                .actualStart(FixtureClock.NOW);
    }

    // ── Pre-built TenantScenario work orders (ids match seed-two-tenants.sql) ─

    /** ACME WO-1 — CREATED, no technician (id 301). Tests dispatcher unassigned list. */
    public static WorkOrder.WorkOrderBuilder acmeWo1(Customer acme, Site acmeHq, AppUser manager) {
        return aCreatedWorkOrder(acme, acmeHq, manager)
                .id(301L)
                .workOrderNumber("WO-ACME-0001")
                .title("ACME HVAC Repair")
                .description("HVAC system not cooling properly in building A")
                .priority(Priority.HIGH);
    }

    /** ACME WO-2 — ASSIGNED to shared technician (id 302). Tests technician's assigned jobs. */
    public static WorkOrder.WorkOrderBuilder acmeWo2(Customer acme, Site acmeWarehouse,
                                                       AppUser manager, AppUser technician) {
        return anAssignedWorkOrder(acme, acmeWarehouse, manager, technician)
                .id(302L)
                .workOrderNumber("WO-ACME-0002")
                .title("ACME Pump Replacement")
                .description("Replace failing pump in warehouse B")
                .priority(Priority.CRITICAL);
    }

    /** ACME WO-3 — IN_PROGRESS with shared technician (id 303). Tests cross-tenant read. */
    public static WorkOrder.WorkOrderBuilder acmeWo3(Customer acme, Site acmeHq,
                                                       AppUser manager, AppUser technician) {
        return anInProgressWorkOrder(acme, acmeHq, manager, technician)
                .id(303L)
                .workOrderNumber("WO-ACME-0003")
                .title("ACME Electrical Inspection")
                .description("Annual electrical safety inspection")
                .priority(Priority.MEDIUM);
    }

    /** GLOBEX WO-1 — CREATED, no technician (id 304). Same dispatcher screen scenario. */
    public static WorkOrder.WorkOrderBuilder globexWo1(Customer globex, Site globexPlant, AppUser manager) {
        return aCreatedWorkOrder(globex, globexPlant, manager)
                .id(304L)
                .workOrderNumber("WO-GLOBEX-0001")
                .title("GLOBEX Assembly Line Repair")
                .description("Conveyor belt broken on line 3")
                .priority(Priority.CRITICAL);
    }

    /** GLOBEX WO-2 — IN_PROGRESS with shared technician (id 305). Cross-tenant assignment. */
    public static WorkOrder.WorkOrderBuilder globexWo2(Customer globex, Site globexPlant,
                                                         AppUser manager, AppUser technician) {
        return anInProgressWorkOrder(globex, globexPlant, manager, technician)
                .id(305L)
                .workOrderNumber("WO-GLOBEX-0002")
                .title("GLOBEX CNC Calibration")
                .description("Monthly CNC machine calibration PM service")
                .priority(Priority.HIGH);
    }
}
