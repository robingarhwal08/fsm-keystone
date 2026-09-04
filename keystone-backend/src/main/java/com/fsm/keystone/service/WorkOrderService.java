package com.fsm.keystone.service;

import com.fsm.keystone.dto.AssignTechnicianRequest;
import com.fsm.keystone.dto.CreateWorkOrderRequest;
import com.fsm.keystone.dto.PartUsageRequest;
import com.fsm.keystone.dto.PartUsageResponse;
import com.fsm.keystone.dto.StatusUpdateRequest;
import com.fsm.keystone.dto.TimeLogRequest;
import com.fsm.keystone.dto.TimeLogResponse;
import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.entity.Part;
import com.fsm.keystone.entity.PartUsage;
import com.fsm.keystone.entity.Site;
import com.fsm.keystone.dto.StatusHistoryResponse;
import com.fsm.keystone.entity.StatusHistory;
import com.fsm.keystone.entity.TimeLog;
import com.fsm.keystone.entity.WorkOrder;
import com.fsm.keystone.enums.PartUsageStatus;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.enums.SlaStatus;
import com.fsm.keystone.enums.WorkOrderStatus;
import com.fsm.keystone.exception.BusinessException;
import com.fsm.keystone.exception.IllegalTransitionException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final TimeLogPhotoService timeLogPhotoService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final SlaService slaService;
    private final CascadeDeleteService cascadeDeleteService;

    @Transactional
    public WorkOrder createWorkOrder(CreateWorkOrderRequest req) {
        AppUser actor = currentUser();
        if (actor != null) {
            actor = userRepo.findById(actor.getId()).orElse(actor);
        }

        Site site = siteRepo.findById(req.siteId())
                .orElseThrow(() ->
                        new RuntimeException("Site not found with id: " + req.siteId()));

        Customer customer;
        if (actor != null && actor.getRole() == Role.CUSTOMER) {
            customer = actor.getCustomer() != null ? actor.getCustomer() : site.getCustomer();
            if (customer == null) {
                throw new BusinessException("This customer account is not linked to an organisation");
            }
            if (!site.getCustomer().getId().equals(customer.getId())) {
                throw new BusinessException("You can only raise requests for your own sites");
            }
        } else {
            Long customerId = req.customerId() != null ? req.customerId() : site.getCustomer().getId();
            customer = customerRepo.findById(customerId)
                    .orElseThrow(() ->
                            new RuntimeException("Customer not found with id: " + customerId));
            if (!site.getCustomer().getId().equals(customer.getId())) {
                throw new BusinessException("Site must belong to the selected customer");
            }
        }

        AppUser createdBy;
        if (actor != null && actor.getRole() == Role.CUSTOMER) {
            createdBy = actor;
        } else {
            createdBy = req.createdByUserId() == null
                    ? actor
                    : userRepo.findById(req.createdByUserId())
                    .orElseThrow(() ->
                             new RuntimeException("User not found with id: " + req.createdByUserId()));
        }

        AppUser assignedTechnician = null;
        if (actor == null || actor.getRole() != Role.CUSTOMER) {
            assignedTechnician = req.assignedTechnicianId() == null
                    ? null
                    : userRepo.findById(req.assignedTechnicianId())
                    .orElseThrow(() ->
                                 new RuntimeException("Technician not found with id: " + req.assignedTechnicianId()));
        }

        LocalDateTime now = LocalDateTime.now();
        WorkOrderStatus initial = assignedTechnician == null ? WorkOrderStatus.NEW : WorkOrderStatus.ASSIGNED;
        var priority = req.priority() == null ? com.fsm.keystone.enums.Priority.MEDIUM : req.priority();

        WorkOrder workOrder = WorkOrder.builder()
                .title(req.title())
                .description(req.description())
                .customer(customer)
                .site(site)
                .createdBy(createdBy)
                .assignedTechnician(assignedTechnician)
                .priority(priority)
                .status(initial)
                .scheduledStart(req.scheduledStart())
                .scheduledEnd(req.scheduledEnd())
                .workOrderNumber(nextWorkOrderNumber())
                .createdAt(now)
                .updatedAt(now)
                .slaDueAt(slaService.dueDateFor(priority, now))
                .slaStatus(SlaStatus.ON_TRACK)
                .totalPartsCost(BigDecimal.ZERO)
                .totalMinutes(0)
                .build();

        WorkOrder saved = workRepo.save(workOrder);
        recordHistory(saved, null, initial, createdBy, "Work order created");
        saved.setSlaStatus(slaService.evaluate(saved, now));
        saved = workRepo.save(saved);
        notificationService.notifyWorkCreated(saved, createdBy);
        return saved;
    }

    @Transactional
    public List<WorkOrder> getAllWorkOrders() {
        AppUser actor = currentUser();
        List<WorkOrder> list;
        if (actor != null && actor.getRole() == Role.TECHNICIAN) {
            list = workRepo.findByAssignedTechnician_Id(actor.getId());
        } else if (actor != null && actor.getRole() == Role.CUSTOMER) {
            AppUser customerUser = userRepo.findByEmailWithCustomer(actor.getEmail()).orElse(actor);
            if (customerUser.getCustomer() != null) {
                list = workRepo.findByCustomer_Id(customerUser.getCustomer().getId());
            } else {
                list = workRepo.findByCreatedBy_Id(actor.getId());
            }
        } else {
            list = workRepo.findAll();
        }
        for (WorkOrder workOrder : list) {
            SlaStatus before = workOrder.getSlaStatus();
            LocalDateTime due = workOrder.getSlaDueAt();
            slaService.ensureSla(workOrder);
            if (due == null || before != workOrder.getSlaStatus()) {
                workRepo.save(workOrder);
            }
        }
        return list;
    }

    public Map<String, List<WorkOrder>> getBoard() {
        Map<String, List<WorkOrder>> board = new LinkedHashMap<>();
        for (WorkOrderStatus status : List.of(
                WorkOrderStatus.NEW,
                WorkOrderStatus.ASSIGNED,
                WorkOrderStatus.IN_PROGRESS,
                WorkOrderStatus.ON_HOLD,
                WorkOrderStatus.COMPLETED,
                WorkOrderStatus.CLOSED,
                WorkOrderStatus.CANCELLED)) {
            board.put(status.name(), new ArrayList<>());
        }
        for (WorkOrder workOrder : getAllWorkOrders()) {
            String key = workOrder.getStatus() == null ? "NEW" : workOrder.getStatus().canonical().name();
            board.computeIfAbsent(key, ignored -> new ArrayList<>()).add(workOrder);
        }
        return board;
    }

    @Transactional
    public WorkOrder getWorkOrderById(Long id) {
        WorkOrder workOrder = workRepo.findByIdWithDetails(id)
                .orElseThrow(() ->
                        new RuntimeException("Work order not found with id: " + id));
        assertCanView(workOrder);
        slaService.ensureSla(workOrder);
        return workOrder;
    }

    @Transactional
    public WorkOrder assignTechnician(Long id, AssignTechnicianRequest req) {
        WorkOrder workOrder = getWorkOrderById(id);
        assertEditable(workOrder);

        AppUser technician = userRepo.findById(req.technicianId())
                .orElseThrow(() ->
                        new RuntimeException("Technician not found with id: " + req.technicianId()));

        if (technician.getRole() != Role.TECHNICIAN) {
            throw new BusinessException("Assignee must be a technician");
        }

        WorkOrderStatus oldStatus = workOrder.getStatus();
        AppUser actor = currentUser();
        WorkOrderLifecycle.assertAllowed(oldStatus, WorkOrderStatus.ASSIGNED,
                actor == null ? Role.DISPATCHER : actor.getRole(), false);

        workOrder.setAssignedTechnician(technician);
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);

        WorkOrder savedWorkOrder = workRepo.save(workOrder);
        recordHistory(savedWorkOrder, oldStatus, WorkOrderStatus.ASSIGNED, actor, "Technician assigned");
        notificationService.notifyAssigned(savedWorkOrder, technician, actor);
        return savedWorkOrder;
    }

    @Transactional
    public WorkOrder updateStatus(Long id, StatusUpdateRequest req) {
        WorkOrder workOrder = getWorkOrderById(id);
        WorkOrderStatus oldStatus = workOrder.getStatus();
        WorkOrderStatus target = req.status();

        AppUser actor = req.changedByUserId() == null
                ? currentUser()
                : userRepo.findById(req.changedByUserId()).orElse(currentUser());

        boolean assignee = workOrder.getAssignedTechnician() != null
                && actor != null
                && workOrder.getAssignedTechnician().getId().equals(actor.getId());

        WorkOrderLifecycle.assertAllowed(
                oldStatus,
                target,
                actor == null ? Role.MANAGER : actor.getRole(),
                assignee);

        if (actor != null && actor.getRole() == Role.TECHNICIAN && !assignee) {
            throw new IllegalTransitionException("Technicians can only update jobs assigned to them");
        }

        workOrder.setStatus(target.canonical() == WorkOrderStatus.NEW ? WorkOrderStatus.NEW : target.canonical());
        if (target.canonical() == WorkOrderStatus.IN_PROGRESS && workOrder.getActualStart() == null) {
            workOrder.setActualStart(LocalDateTime.now());
        }
        if (target.canonical() == WorkOrderStatus.COMPLETED || target.canonical() == WorkOrderStatus.CLOSED) {
            workOrder.setActualEnd(LocalDateTime.now());
        }

        WorkOrder savedWorkOrder = workRepo.save(workOrder);
        recordHistory(savedWorkOrder, oldStatus, savedWorkOrder.getStatus(), actor, req.comment());
        slaService.ensureSla(savedWorkOrder);
        workRepo.save(savedWorkOrder);
        notificationService.notifyStatusChanged(
                savedWorkOrder,
                oldStatus == null ? "NEW" : oldStatus.canonical().name(),
                savedWorkOrder.getStatus().canonical().name(),
                actor);
        return savedWorkOrder;
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getWorkOrderHistory(Long id) {
        getWorkOrderById(id);
        return historyRepo.findByWorkOrderIdOrderByChangedAtAsc(id)
                .stream()
                .map(history -> new StatusHistoryResponse(
                        history.getId(),
                        history.getOldStatus() == null ? null : history.getOldStatus().name(),
                        history.getNewStatus() == null ? null : history.getNewStatus().name(),
                        history.getComment(),
                        history.getChangedBy() == null ? "System" : history.getChangedBy().getFullName(),
                        history.getChangedAt()
                ))
                .toList();
    }

    @Transactional
    public PartUsage addPartUsage(Long id, PartUsageRequest req) {
        WorkOrder workOrder = getWorkOrderById(id);
        assertEditable(workOrder);
        assertTechnicianMayLog(workOrder);

        Part part = partRepo.findById(req.partId())
                .orElseThrow(() ->
                        new RuntimeException("Part not found with id: " + req.partId()));

        int quantityUsed = req.quantityUsed() == null
                ? 1
                : req.quantityUsed();

        if (quantityUsed <= 0) {
            throw new BusinessException("Quantity used must be positive");
        }

        PartUsageStatus status = req.usageStatus() == null
                ? PartUsageStatus.USED
                : req.usageStatus();

        if (status == PartUsageStatus.USED) {
            applyInventoryDeduction(part, quantityUsed);
        }

        BigDecimal unitPrice = part.getUnitPrice() == null
                ? BigDecimal.ZERO
                : part.getUnitPrice();

        BigDecimal totalCost = unitPrice.multiply(
                BigDecimal.valueOf(quantityUsed)
        );

        AppUser usedBy = req.usedByUserId() == null
                ? currentUser()
                : userRepo.findById(req.usedByUserId())
                .orElseThrow(() ->
                             new RuntimeException("User not found with id: " + req.usedByUserId()));

        PartUsage partUsage = PartUsage.builder()
                .workOrder(workOrder)
                .part(part)
                .quantityUsed(quantityUsed)
                .unitPriceAtUsage(unitPrice)
                .totalCost(totalCost)
                .usageStatus(status)
                .usedBy(usedBy)
                .build();

        PartUsage saved = partUsageRepo.save(partUsage);
        if (status == PartUsageStatus.USED) {
            addPartsCost(workOrder, totalCost);
        }
        return saved;
    }

    @Transactional
    public PartUsage confirmPartUsage(Long usageId) {
        AppUser actor = currentUser();
        if (actor == null || (actor.getRole() != Role.MANAGER && actor.getRole() != Role.DISPATCHER)) {
            throw new BusinessException("Only managers and dispatchers can confirm part usage");
        }
        PartUsage usage = partUsageRepo.findById(usageId)
                .orElseThrow(() -> new RuntimeException("Part usage not found with id: " + usageId));
        if (usage.getUsageStatus() == PartUsageStatus.USED) {
            throw new BusinessException("This part usage is already marked used");
        }
        Part part = usage.getPart();
        applyInventoryDeduction(part, usage.getQuantityUsed());
        usage.setUsageStatus(PartUsageStatus.USED);
        PartUsage saved = partUsageRepo.save(usage);
        addPartsCost(usage.getWorkOrder(), usage.getTotalCost());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PartUsageResponse> listPartUsage() {
        AppUser actor = currentUser();
        List<PartUsage> usages;
        if (actor != null && actor.getRole() == Role.TECHNICIAN) {
            usages = partUsageRepo.findByUsedByIdWithDetails(actor.getId());
        } else {
            usages = partUsageRepo.findAllWithDetails();
        }
        return usages.stream().map(this::toPartUsageResponse).toList();
    }

    private void applyInventoryDeduction(Part part, int quantityUsed) {
        int stock = part.getStockQuantity() == null ? 0 : part.getStockQuantity();
        if (stock < quantityUsed) {
            throw new BusinessException("Stock cannot go negative");
        }
        part.setStockQuantity(stock - quantityUsed);
        partRepo.save(part);
    }

    private void addPartsCost(WorkOrder workOrder, BigDecimal totalCost) {
        if (totalCost == null) {
            return;
        }
        BigDecimal current = workOrder.getTotalPartsCost() == null ? BigDecimal.ZERO : workOrder.getTotalPartsCost();
        workOrder.setTotalPartsCost(current.add(totalCost));
        workRepo.save(workOrder);
    }

    private PartUsageResponse toPartUsageResponse(PartUsage usage) {
        Part part = usage.getPart();
        WorkOrder workOrder = usage.getWorkOrder();
        String siteName = workOrder == null || workOrder.getSite() == null
                ? null
                : workOrder.getSite().getSiteName();
        String technician = usage.getUsedBy() != null
                ? usage.getUsedBy().getFullName()
                : (workOrder != null && workOrder.getAssignedTechnician() != null
                        ? workOrder.getAssignedTechnician().getFullName()
                        : "System");
        return new PartUsageResponse(
                usage.getId(),
                workOrder == null ? null : workOrder.getId(),
                workOrder == null ? null : workOrder.getWorkOrderNumber(),
                workOrder == null ? null : workOrder.getTitle(),
                siteName,
                part == null ? null : part.getId(),
                part == null ? null : part.getPartName(),
                part == null ? null : part.getPartNumber(),
                usage.getQuantityUsed(),
                usage.getUnitPriceAtUsage(),
                usage.getTotalCost(),
                part == null ? null : part.getStockQuantity(),
                usage.getUsageStatus() == null ? PartUsageStatus.USED : usage.getUsageStatus(),
                technician,
                technician,
                usage.getUsedAt()
        );
    }

    @Transactional
    public TimeLogResponse addTimeLog(Long id, TimeLogRequest req, List<MultipartFile> images) {
        WorkOrder workOrder = getWorkOrderById(id);
        assertEditable(workOrder);
        assertTechnicianMayLog(workOrder);

        AppUser technician = userRepo.findById(req.technicianId())
                .orElseThrow(() ->
                        new RuntimeException("Technician not found with id: " + req.technicianId()));

        BigDecimal hoursSpent = BigDecimal.ZERO;
        int minutes = 0;

        if (req.startTime() != null && req.endTime() != null) {
            if (!req.endTime().isAfter(req.startTime())) {
                throw new BusinessException("End time must be after start time");
            }
            minutes = (int) Duration.between(req.startTime(), req.endTime()).toMinutes();
            hoursSpent = BigDecimal.valueOf(minutes / 60.0).setScale(2, RoundingMode.HALF_UP);
        }

        TimeLog timeLog = TimeLog.builder()
                .workOrder(workOrder)
                .technician(technician)
                .startTime(req.startTime())
                .endTime(req.endTime())
                .hoursSpent(hoursSpent)
                .minutesSpent(minutes)
                .workDescription(req.workDescription())
                .build();

        TimeLog saved = timeLogRepo.save(timeLog);
        timeLogPhotoService.attachPhotos(saved, images);

        int total = workOrder.getTotalMinutes() == null ? 0 : workOrder.getTotalMinutes();
        workOrder.setTotalMinutes(total + minutes);
        workRepo.save(workOrder);

        return toTimeLogResponse(timeLogRepo.findByIdWithDetails(saved.getId()).orElse(saved));
    }

    @Transactional
    public TimeLogResponse addTimeLog(Long id, TimeLogRequest req) {
        return addTimeLog(id, req, List.of());
    }

    private TimeLogResponse toTimeLogResponse(TimeLog log) {
        return new TimeLogResponse(
                log.getId(),
                log.getWorkOrder() == null ? null : log.getWorkOrder().getId(),
                log.getWorkOrder() == null ? null : log.getWorkOrder().getWorkOrderNumber(),
                log.getWorkOrder() == null ? null : log.getWorkOrder().getTitle(),
                log.getTechnician() == null ? null : log.getTechnician().getFullName(),
                log.getStartTime(),
                log.getEndTime(),
                log.getHoursSpent(),
                log.getMinutesSpent(),
                log.getWorkDescription(),
                timeLogPhotoService.mapPhotos(log.getPhotos())
        );
    }

    @Transactional(readOnly = true)
    public List<TimeLogResponse> listTimeLogs() {
        AppUser actor = currentUser();
        List<TimeLog> logs;
        if (actor != null && actor.getRole() == Role.TECHNICIAN) {
            logs = timeLogRepo.findByTechnicianIdWithDetails(actor.getId());
        } else {
            logs = timeLogRepo.findAllWithDetails();
        }
        return logs.stream().map(this::toTimeLogResponse).toList();
    }

    @Transactional
    public WorkOrder updateWorkOrder(
            Long id,
            CreateWorkOrderRequest req) {

        WorkOrder wo = getWorkOrderById(id);
        assertEditable(wo);

        wo.setTitle(req.title());
        wo.setDescription(req.description());
        wo.setPriority(req.priority());
        if (req.priority() != null && wo.getCreatedAt() != null) {
            wo.setSlaDueAt(slaService.dueDateFor(req.priority(), wo.getCreatedAt()));
            wo.setSlaStatus(slaService.evaluate(wo, LocalDateTime.now()));
        }

        Customer customer = customerRepo.findById(req.customerId())
                .orElseThrow(() ->
                        new RuntimeException("Customer not found"));

        wo.setCustomer(customer);

        Site site = siteRepo.findById(req.siteId())
                .orElseThrow(() ->
                        new RuntimeException("Site not found"));

        if (!site.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessException("Site must belong to the selected customer");
        }

        wo.setSite(site);

        if (req.assignedTechnicianId() != null) {
            AppUser technician = userRepo.findById(req.assignedTechnicianId())
                    .orElseThrow(() ->
                            new RuntimeException("Technician not found"));
            wo.setAssignedTechnician(technician);
            if (wo.getStatus() == null || wo.getStatus().canonical() == WorkOrderStatus.NEW) {
                WorkOrderStatus old = wo.getStatus();
                wo.setStatus(WorkOrderStatus.ASSIGNED);
                recordHistory(wo, old, WorkOrderStatus.ASSIGNED, currentUser(), "Technician assigned");
            }
        } else {
            wo.setAssignedTechnician(null);
        }

        return workRepo.save(wo);
    }

    public void deleteWorkOrder(Long id) {
        getWorkOrderById(id);
        cascadeDeleteService.deleteWorkOrder(id);
    }

    private void assertEditable(WorkOrder workOrder) {
        AppUser actor = currentUser();
        if (actor != null && (actor.getRole() == Role.MANAGER || actor.getRole() == Role.DISPATCHER)) {
            return;
        }
        if (workOrder.getStatus() != null && workOrder.getStatus().isTerminal()) {
            throw new BusinessException("Closed or cancelled work orders are immutable");
        }
    }

    private void assertTechnicianMayLog(WorkOrder workOrder) {
        AppUser actor = currentUser();
        if (actor != null && actor.getRole() == Role.TECHNICIAN) {
            if (workOrder.getAssignedTechnician() == null
                    || !workOrder.getAssignedTechnician().getId().equals(actor.getId())) {
                throw new BusinessException("Technicians can only log against their assigned jobs");
            }
        }
    }

    private void assertCanView(WorkOrder workOrder) {
        AppUser actor = currentUser();
        if (actor == null) {
            return;
        }
        if (actor.getRole() == Role.TECHNICIAN
                && (workOrder.getAssignedTechnician() == null
                || !workOrder.getAssignedTechnician().getId().equals(actor.getId()))) {
            throw new BusinessException("Technicians can only view assigned jobs");
        }
        if (actor.getRole() == Role.CUSTOMER) {
            if (!customerOwnsWorkOrder(workOrder, actor)) {
                throw new BusinessException("Customers can only view their own work orders");
            }
        }
    }

    private boolean customerOwnsWorkOrder(WorkOrder workOrder, AppUser actor) {
        if (workOrder.getCreatedBy() != null) {
            return workOrder.getCreatedBy().getId().equals(actor.getId());
        }
        return historyRepo.findByWorkOrderIdOrderByChangedAtAsc(workOrder.getId())
                .stream()
                .filter(h -> h.getChangedBy() != null)
                .findFirst()
                .map(h -> h.getChangedBy().getId().equals(actor.getId()))
                .orElse(false);
    }

    private void recordHistory(WorkOrder workOrder, WorkOrderStatus from, WorkOrderStatus to, AppUser actor, String comment) {
        historyRepo.save(StatusHistory.builder()
                .workOrder(workOrder)
                .oldStatus(from)
                .newStatus(to)
                .changedBy(actor)
                .comment(comment)
                .build());
    }

    private String nextWorkOrderNumber() {
        String prefix = "WO-" + Year.now().getValue() + "-";
        long seq = workRepo.countByWorkOrderNumberStartingWith(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

    private AppUser currentUser() {
        try {
            return currentUserService.requireUser();
        } catch (Exception ex) {
            return null;
        }
    }
}
