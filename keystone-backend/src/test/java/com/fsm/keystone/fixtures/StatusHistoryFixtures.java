package com.fsm.keystone.fixtures;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.StatusHistory;
import com.fsm.keystone.entity.WorkOrder;
import com.fsm.keystone.enums.WorkOrderStatus;

/**
 * Object-mother factory for {@link StatusHistory} entities.
 *
 * <p>Minimum valid shape: {@code workOrder} is required ({@code optional = false} on FK).
 * {@code changedAt} is set via {@code @PrePersist} but populated explicitly here
 * for determinism in unit tests.</p>
 */
public final class StatusHistoryFixtures {

    private StatusHistoryFixtures() {}

    /** Returns a builder for a generic CREATED → ASSIGNED transition. */
    public static StatusHistory.StatusHistoryBuilder aStatusTransition(
            WorkOrder workOrder, AppUser changedBy) {
        return StatusHistory.builder()
                .oldStatus(WorkOrderStatus.CREATED)
                .newStatus(WorkOrderStatus.ASSIGNED)
                .comment("Status updated by fixture")
                .changedAt(FixtureClock.NOW)
                .workOrder(workOrder)
                .changedBy(changedBy);
    }

    /** Returns a builder for a CREATED → ASSIGNED history entry for the given work order. */
    public static StatusHistory.StatusHistoryBuilder assignmentHistory(
            WorkOrder workOrder, AppUser assignedBy) {
        return aStatusTransition(workOrder, assignedBy)
                .comment("Technician assigned");
    }

    /** Returns a builder for an ASSIGNED → IN_PROGRESS history entry. */
    public static StatusHistory.StatusHistoryBuilder startedHistory(
            WorkOrder workOrder, AppUser changedBy) {
        return aStatusTransition(workOrder, changedBy)
                .oldStatus(WorkOrderStatus.ASSIGNED)
                .newStatus(WorkOrderStatus.IN_PROGRESS)
                .comment("Work order started");
    }
}
