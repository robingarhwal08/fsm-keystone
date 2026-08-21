package com.fsm.keystone.service;

import com.fsm.keystone.dto.NotificationResponse;
import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.entity.Notification;
import com.fsm.keystone.entity.WorkOrder;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.repository.NotificationRepository;
import com.fsm.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public void notify(AppUser user, WorkOrder workOrder, String type, String message) {
        if (user == null) {
            return;
        }
        notificationRepository.save(Notification.builder()
                .user(user)
                .workOrder(workOrder)
                .type(type)
                .message(message)
                .readFlag(false)
                .build());
    }

    public void notifyRoles(WorkOrder workOrder, String type, String message, AppUser actor, Role... roles) {
        Set<Long> seen = new HashSet<>();
        if (actor != null) {
            seen.add(actor.getId());
        }
        for (Role role : roles) {
            for (AppUser user : userRepository.findByRole(role)) {
                if (seen.add(user.getId())) {
                    notify(user, workOrder, type, message);
                }
            }
        }
    }

    public void notifyCustomerOrg(WorkOrder workOrder, String type, String message, AppUser actor) {
        Customer customer = workOrder.getCustomer();
        if (customer == null) {
            return;
        }
        for (AppUser user : userRepository.findByCustomer_Id(customer.getId())) {
            if (user.getRole() != Role.CUSTOMER) {
                continue;
            }
            if (actor != null && actor.getId().equals(user.getId())) {
                continue;
            }
            notify(user, workOrder, type, message);
        }
    }

    public void notifyWorkCreated(WorkOrder workOrder, AppUser actor) {
        String code = workOrder.getWorkOrderNumber();
        notifyRoles(workOrder, "CREATED",
                "New work order " + code + " was created: " + workOrder.getTitle(),
                actor, Role.MANAGER, Role.DISPATCHER);
        notifyCustomerOrg(workOrder, "CREATED",
                "Your request " + code + " was submitted and is being tracked.",
                actor);
        if (workOrder.getAssignedTechnician() != null
                && (actor == null || !actor.getId().equals(workOrder.getAssignedTechnician().getId()))) {
            notify(workOrder.getAssignedTechnician(), workOrder, "ASSIGNMENT",
                    "Work order " + code + " was assigned to you.");
        }
    }

    public void notifyAssigned(WorkOrder workOrder, AppUser technician, AppUser actor) {
        String code = workOrder.getWorkOrderNumber();
        if (technician != null && (actor == null || !actor.getId().equals(technician.getId()))) {
            notify(technician, workOrder, "ASSIGNMENT",
                    "Work order " + code + " was assigned to you.");
        }
        notifyRoles(workOrder, "ASSIGNMENT",
                code + " was assigned to " + (technician == null ? "a technician" : technician.getFullName()) + ".",
                actor, Role.MANAGER, Role.DISPATCHER);
        notifyCustomerOrg(workOrder, "ASSIGNMENT",
                "A technician was assigned to your request " + code + ".",
                actor);
    }

    public void notifyStatusChanged(WorkOrder workOrder, String from, String to, AppUser actor) {
        String code = workOrder.getWorkOrderNumber();
        String message = code + " moved from " + from + " to " + to + ".";
        notifyRoles(workOrder, "STATUS", message, actor, Role.MANAGER, Role.DISPATCHER);
        if (workOrder.getAssignedTechnician() != null
                && (actor == null || !actor.getId().equals(workOrder.getAssignedTechnician().getId()))) {
            notify(workOrder.getAssignedTechnician(), workOrder, "STATUS",
                    "Your job " + message);
        }
        notifyCustomerOrg(workOrder, "STATUS",
                "Your request " + code + " is now " + to + ".",
                actor);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> myNotifications() {
        AppUser user = currentUserService.requireUser();
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(n -> new NotificationResponse(
                        n.getId(),
                        n.getMessage(),
                        n.getType(),
                        n.getReadFlag(),
                        n.getCreatedAt(),
                        n.getWorkOrder() == null ? null : n.getWorkOrder().getWorkOrderNumber()
                ))
                .toList();
    }

    public NotificationResponse markRead(Long id) {
        AppUser user = currentUserService.requireUser();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Notification not found");
        }
        notification.setReadFlag(true);
        Notification saved = notificationRepository.save(notification);
        return new NotificationResponse(
                saved.getId(),
                saved.getMessage(),
                saved.getType(),
                saved.getReadFlag(),
                saved.getCreatedAt(),
                saved.getWorkOrder() == null ? null : saved.getWorkOrder().getWorkOrderNumber()
        );
    }
}
