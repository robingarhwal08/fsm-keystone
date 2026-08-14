package com.fsm.keystone.service;

import com.fsm.keystone.enums.Role;
import com.fsm.keystone.enums.WorkOrderStatus;
import com.fsm.keystone.exception.IllegalTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class WorkOrderLifecycle {

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED = new EnumMap<>(WorkOrderStatus.class);

    static {
        ALLOWED.put(WorkOrderStatus.NEW, EnumSet.of(WorkOrderStatus.ASSIGNED, WorkOrderStatus.CANCELLED));
        ALLOWED.put(WorkOrderStatus.ASSIGNED, EnumSet.of(
                WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.ASSIGNED, WorkOrderStatus.CANCELLED));
        ALLOWED.put(WorkOrderStatus.IN_PROGRESS, EnumSet.of(
                WorkOrderStatus.ON_HOLD, WorkOrderStatus.COMPLETED, WorkOrderStatus.CANCELLED));
        ALLOWED.put(WorkOrderStatus.ON_HOLD, EnumSet.of(
                WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED));
        ALLOWED.put(WorkOrderStatus.COMPLETED, EnumSet.of(WorkOrderStatus.CLOSED));
        ALLOWED.put(WorkOrderStatus.CLOSED, EnumSet.noneOf(WorkOrderStatus.class));
        ALLOWED.put(WorkOrderStatus.CANCELLED, EnumSet.noneOf(WorkOrderStatus.class));
    }

    private WorkOrderLifecycle() {
    }

    public static void assertAllowed(WorkOrderStatus from, WorkOrderStatus to, Role role, boolean isAssignee) {
        WorkOrderStatus source = from == null ? WorkOrderStatus.NEW : from.canonical();
        WorkOrderStatus target = to.canonical();

        if (source == target && target != WorkOrderStatus.ASSIGNED) {
            return;
        }

        Set<WorkOrderStatus> next = ALLOWED.getOrDefault(source, EnumSet.noneOf(WorkOrderStatus.class));
        if (!next.contains(target)) {
            throw new IllegalTransitionException(
                    "Illegal status transition from " + source + " to " + target);
        }

        if (target == WorkOrderStatus.CLOSED && role != Role.MANAGER) {
            throw new IllegalTransitionException("Only a manager can close a work order");
        }

        if ((target == WorkOrderStatus.IN_PROGRESS
                || target == WorkOrderStatus.ON_HOLD
                || target == WorkOrderStatus.COMPLETED)
                && role == Role.TECHNICIAN
                && !isAssignee) {
            throw new IllegalTransitionException("Technicians can only update jobs assigned to them");
        }

        if (role == Role.CUSTOMER) {
            throw new IllegalTransitionException("Customers cannot change work-order status");
        }

        if (role == Role.TECHNICIAN && (target == WorkOrderStatus.CANCELLED || target == WorkOrderStatus.ASSIGNED)) {
            throw new IllegalTransitionException("Technicians cannot reassign or cancel jobs");
        }
    }
}
