package com.fsm.keystone.service;

import com.fsm.keystone.dto.AssignTechnicianRequest;
import com.fsm.keystone.dto.CreateWorkOrderRequest;
import com.fsm.keystone.dto.PartUsageRequest;
import com.fsm.keystone.dto.StatusUpdateRequest;
import com.fsm.keystone.dto.TimeLogRequest;
import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.entity.Part;
import com.fsm.keystone.entity.PartUsage;
import com.fsm.keystone.entity.Site;
import com.fsm.keystone.entity.StatusHistory;
import com.fsm.keystone.entity.TimeLog;
import com.fsm.keystone.entity.WorkOrder;
import com.fsm.keystone.enums.WorkOrderStatus;
import com.fsm.keystone.repository.UserRepository;
import com.fsm.keystone.repository.CustomerRepository;
import com.fsm.keystone.repository.PartRepository;
import com.fsm.keystone.repository.PartUsageRepository;
import com.fsm.keystone.repository.SiteRepository;
import com.fsm.keystone.repository.StatusHistoryRepository;
import com.fsm.keystone.repository.TimeLogRepository;
import com.fsm.keystone.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workRepo;
    private final CustomerRepository customerRepo;
    private final SiteRepository siteRepo;
    private final UserRepository userRepo;
    private final StatusHistoryRepository historyRepo;
    private final PartRepository partRepo;
    private final PartUsageRepository partUsageRepo;
    private final TimeLogRepository timeLogRepo;

    public WorkOrder createWorkOrder(CreateWorkOrderRequest req) {

        Customer customer = customerRepo.findById(req.customerId())
                .orElseThrow(() ->
                        new RuntimeException("Customer not found with id: " + req.customerId()));

        Site site = siteRepo.findById(req.siteId())
                .orElseThrow(() ->
                        new RuntimeException("Site not found with id: " + req.siteId()));

        AppUser createdBy = req.createdByUserId() == null
                ? null
                : userRepo.findById(req.createdByUserId())
                .orElseThrow(() ->
                             new RuntimeException("User not found with id: " + req.createdByUserId()));

        AppUser assignedTechnician = req.assignedTechnicianId() == null
                ? null
                : userRepo.findById(req.assignedTechnicianId())
                .orElseThrow(() ->
                             new RuntimeException("Technician not found with id: " + req.assignedTechnicianId()));

        WorkOrder workOrder = WorkOrder.builder()
                .title(req.title())
                .description(req.description())
                .customer(customer)
                .site(site)
                .createdBy(createdBy)
                .assignedTechnician(assignedTechnician)
                .priority(req.priority())
                .scheduledStart(req.scheduledStart())
                .scheduledEnd(req.scheduledEnd())
                .build();

        return workRepo.save(workOrder);
    }

    public List<WorkOrder> getAllWorkOrders() {

        return workRepo.findAll();
    }

    public WorkOrder getWorkOrderById(Long id) {

        return workRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found with id: " + id));
    }

    public WorkOrder assignTechnician(Long id, AssignTechnicianRequest req) {

        WorkOrder workOrder = workRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found with id: " + id));

        AppUser technician = userRepo.findById(req.technicianId())
                .orElseThrow(() ->
                        new RuntimeException("Technician not found with id: " + req.technicianId()));

        WorkOrderStatus oldStatus = workOrder.getStatus();

        workOrder.setAssignedTechnician(technician);
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);

        WorkOrder savedWorkOrder = workRepo.save(workOrder);

        StatusHistory history = StatusHistory.builder()
                .workOrder(savedWorkOrder)
                .oldStatus(oldStatus)
                .newStatus(WorkOrderStatus.ASSIGNED)
                .changedBy(savedWorkOrder.getAssignedTechnician())
                .comment("Technician assigned")
                .build();

        historyRepo.save(history);

        return savedWorkOrder;
    }

    public WorkOrder updateStatus(Long id, StatusUpdateRequest req) {

        WorkOrder workOrder = workRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found with id: " + id));

        WorkOrderStatus oldStatus = workOrder.getStatus();

        workOrder.setStatus(req.status());

        WorkOrder savedWorkOrder = workRepo.save(workOrder);

        AppUser changedBy = req.changedByUserId() == null
                ? null
                : userRepo.findById(req.changedByUserId())
                .orElseThrow(() ->
                             new RuntimeException("User not found with id: " + req.changedByUserId()));

        StatusHistory history = StatusHistory.builder()
                .workOrder(savedWorkOrder)
                .oldStatus(oldStatus)
                .newStatus(req.status())
                .changedBy(changedBy)
                .comment(req.comment())
                .build();

        historyRepo.save(history);

        return savedWorkOrder;
    }

    public List<StatusHistory> getWorkOrderHistory(Long id) {

        return historyRepo.findAll()
                .stream()
                .filter(history -> history.getWorkOrder().getId().equals(id))
                .toList();
    }

    public PartUsage addPartUsage(Long id, PartUsageRequest req) {

        WorkOrder workOrder = workRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found with id: " + id));

        Part part = partRepo.findById(req.partId())
                .orElseThrow(() ->
                        new RuntimeException("Part not found with id: " + req.partId()));

        int quantityUsed = req.quantityUsed() == null
                ? 1
                : req.quantityUsed();

        if (part.getStockQuantity() != null) {
            part.setStockQuantity(part.getStockQuantity() - quantityUsed);
        }

        partRepo.save(part);

        BigDecimal unitPrice = part.getUnitPrice() == null
                ? BigDecimal.ZERO
                : part.getUnitPrice();

        BigDecimal totalCost = unitPrice.multiply(
                BigDecimal.valueOf(quantityUsed)
        );

        AppUser usedBy = req.usedByUserId() == null
                ? null
                : userRepo.findById(req.usedByUserId())
                .orElseThrow(() ->
                             new RuntimeException("User not found with id: " + req.usedByUserId()));

        PartUsage partUsage = PartUsage.builder()
                .workOrder(workOrder)
                .part(part)
                .quantityUsed(quantityUsed)
                .unitPriceAtUsage(unitPrice)
                .totalCost(totalCost)
                .usedBy(usedBy)
                .build();

        return partUsageRepo.save(partUsage);
    }

    public TimeLog addTimeLog(Long id, TimeLogRequest req) {

        WorkOrder workOrder = workRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found with id: " + id));

        AppUser technician = userRepo.findById(req.technicianId())
                .orElseThrow(() ->
                        new RuntimeException("Technician not found with id: " + req.technicianId()));

        BigDecimal hoursSpent = BigDecimal.ZERO;

        if (req.startTime() != null && req.endTime() != null) {

            double hours = Duration.between(
                    req.startTime(),
                    req.endTime()
            ).toMinutes() / 60.0;

            hoursSpent = BigDecimal.valueOf(hours)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        TimeLog timeLog = TimeLog.builder()
                .workOrder(workOrder)
                .technician(technician)
                .startTime(req.startTime())
                .endTime(req.endTime())
                .hoursSpent(hoursSpent)
                .workDescription(req.workDescription())
                .build();

        return timeLogRepo.save(timeLog);
    }

    public WorkOrder updateWorkOrder(
            Long id,
            CreateWorkOrderRequest req) {

        WorkOrder wo = workRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Work Order not found"));

        wo.setTitle(req.title());
        wo.setDescription(req.description());
        wo.setPriority(req.priority());

        // Update Customer
        Customer customer = customerRepo.findById(req.customerId())
                .orElseThrow(() ->
                        new RuntimeException("Customer not found"));

        wo.setCustomer(customer);

        // Update Site
        Site site = siteRepo.findById(req.siteId())
                .orElseThrow(() ->
                        new RuntimeException("Site not found"));

        wo.setSite(site);

        // Update Technician
        if (req.assignedTechnicianId() != null) {

            AppUser technician = userRepo.findById(req.assignedTechnicianId())
                    .orElseThrow(() ->
                            new RuntimeException("Technician not found"));

            wo.setAssignedTechnician(technician);

            wo.setStatus(WorkOrderStatus.ASSIGNED);

        } else {

            wo.setAssignedTechnician(null);

        }

        return workRepo.save(wo);
    }

    public void deleteWorkOrder(Long id) {

        workRepo.deleteById(id);
    }
}