package com.fsm.keystone.controller;

import com.fsm.keystone.dto.PartUsageRequest;
import com.fsm.keystone.dto.PartUsageResponse;
import com.fsm.keystone.entity.PartUsage;
import com.fsm.keystone.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/part-usage")
@RequiredArgsConstructor
public class PartUsageController {

    private final WorkOrderService workOrderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','TECHNICIAN')")
    public List<PartUsageResponse> all() {
        return workOrderService.listPartUsage();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER','TECHNICIAN')")
    public PartUsage create(
            @RequestBody PartUsageRequest request
    ) {
        return workOrderService.addPartUsage(
                request.workOrderId(),
                request
        );
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER')")
    public PartUsage confirm(@PathVariable Long id) {
        return workOrderService.confirmPartUsage(id);
    }
}
