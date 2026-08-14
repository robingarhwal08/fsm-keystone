package com.fsm.keystone.service;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.WorkOrder;
import com.fsm.keystone.enums.Priority;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.enums.SlaStatus;
import com.fsm.keystone.enums.WorkOrderStatus;
import com.fsm.keystone.repository.UserRepository;
import com.fsm.keystone.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlaService {

    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public LocalDateTime dueDateFor(Priority priority, LocalDateTime from) {
        Priority p = priority == null ? Priority.MEDIUM : priority;
        int hours = switch (p) {
            case CRITICAL -> 4;
            case HIGH -> 8;
            case MEDIUM -> 24;
            case LOW -> 72;
        };
        return from.plusHours(hours);
    }

    public SlaStatus evaluate(WorkOrder workOrder, LocalDateTime now) {
        if (workOrder.getSlaDueAt() == null) {
            return SlaStatus.ON_TRACK;
        }
        WorkOrderStatus status = workOrder.getStatus();
        boolean terminal = status != null && (
                status.canonical() == WorkOrderStatus.COMPLETED
                        || status.canonical() == WorkOrderStatus.CLOSED
                        || status.canonical() == WorkOrderStatus.CANCELLED);
        LocalDateTime compareAt = terminal && workOrder.getActualEnd() != null
                ? workOrder.getActualEnd()
                : now;
        if (!compareAt.isBefore(workOrder.getSlaDueAt())) {
            return SlaStatus.BREACHED;
        }
        if (terminal) {
            return SlaStatus.ON_TRACK;
        }
        LocalDateTime start = workOrder.getCreatedAt() != null ? workOrder.getCreatedAt() : compareAt;
        long totalMinutes = java.time.Duration.between(start, workOrder.getSlaDueAt()).toMinutes();
        long remaining = java.time.Duration.between(compareAt, workOrder.getSlaDueAt()).toMinutes();
        if (totalMinutes > 0 && remaining <= Math.max(60, totalMinutes / 4)) {
            return SlaStatus.AT_RISK;
        }
        return SlaStatus.ON_TRACK;
    }

    public void ensureSla(WorkOrder workOrder) {
        LocalDateTime now = LocalDateTime.now();
        if (workOrder.getSlaDueAt() == null) {
            LocalDateTime from = workOrder.getCreatedAt() != null ? workOrder.getCreatedAt() : now;
            workOrder.setSlaDueAt(dueDateFor(workOrder.getPriority(), from));
        }
        workOrder.setSlaStatus(evaluate(workOrder, now));
    }

    @Scheduled(fixedDelay = 60000)
    public void flagBreaches() {
        try {
        List<WorkOrder> open = workOrderRepository.findBySlaDueAtNotNullAndStatusNotIn(
                EnumSet.of(WorkOrderStatus.CLOSED, WorkOrderStatus.CANCELLED));
        LocalDateTime now = LocalDateTime.now();
        List<AppUser> managers = userRepository.findByRole(Role.MANAGER);

        for (WorkOrder workOrder : open) {
            SlaStatus next = evaluate(workOrder, now);
            SlaStatus previous = workOrder.getSlaStatus();
            if (next != previous) {
                workOrder.setSlaStatus(next);
                workOrderRepository.save(workOrder);
                if (next == SlaStatus.AT_RISK || next == SlaStatus.BREACHED) {
                    String message = workOrder.getWorkOrderNumber() + " is " + next;
                    for (AppUser manager : managers) {
                        notificationService.notify(manager, workOrder, "SLA_" + next, message);
                    }
                    for (AppUser dispatcher : userRepository.findByRole(Role.DISPATCHER)) {
                        notificationService.notify(dispatcher, workOrder, "SLA_" + next, message);
                    }
                }
            }
        }
        } catch (Exception ignored) {
            // Scheduler should not crash the application if the database is briefly unavailable.
        }
    }
}
