package com.fsm.keystone.controller;

import com.fsm.keystone.dto.AssignTechnicianRequest;
import com.fsm.keystone.dto.CreateWorkOrderRequest;
import com.fsm.keystone.dto.PartUsageRequest;
import com.fsm.keystone.dto.StatusUpdateRequest;
import com.fsm.keystone.dto.StatusHistoryResponse;
import com.fsm.keystone.dto.TimeLogRequest;
import com.fsm.keystone.dto.TimeLogResponse;
import com.fsm.keystone.entity.PartUsage;
import com.fsm.keystone.entity.WorkOrder;
import com.fsm.keystone.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','CUSTOMER')")
    public WorkOrder create(@Valid @RequestBody CreateWorkOrderRequest req) {
        return workOrderService.createWorkOrder(req);
    }

    @GetMapping
    public List<WorkOrder> all() {
        return workOrderService.getAllWorkOrders();
    }

    @GetMapping("/board")
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER')")
    public Map<String, List<WorkOrder>> board() {
        return workOrderService.getBoard();
    }

    @GetMapping("/{id}")
    public WorkOrder one(@PathVariable Long id) {
        return workOrderService.getWorkOrderById(id);
    }

    @RequestMapping(value = "/{id}/assign", method = {RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER')")
    public WorkOrder assign(
            @PathVariable Long id,
            @RequestBody AssignTechnicianRequest req) {
        return workOrderService.assignTechnician(id, req);
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','TECHNICIAN')")
    public WorkOrder status(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest req) {
        return workOrderService.updateStatus(id, req);
    }

    @GetMapping("/{id}/history")
    public List<StatusHistoryResponse> history(@PathVariable Long id) {
        return workOrderService.getWorkOrderHistory(id);
    }

    @PostMapping("/{id}/parts")
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','TECHNICIAN')")
    public PartUsage addPart(
            @PathVariable Long id,
            @RequestBody PartUsageRequest req) {
        return workOrderService.addPartUsage(id, req);
    }

    @PostMapping({"/{id}/time-logs", "/{id}/time"})
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','TECHNICIAN')")
    public TimeLogResponse addTime(
            @PathVariable Long id,
            @RequestBody TimeLogRequest req) {
        return workOrderService.addTimeLog(id, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER')")
    public WorkOrder update(
            @PathVariable Long id,
            @Valid @RequestBody CreateWorkOrderRequest req) {
        return workOrderService.updateWorkOrder(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER')")
    public void delete(@PathVariable Long id) {
        workOrderService.deleteWorkOrder(id);
    }
}
