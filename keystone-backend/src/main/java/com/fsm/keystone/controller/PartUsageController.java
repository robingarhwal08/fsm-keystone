package com.fsm.keystone.controller;

import com.fsm.keystone.dto.PartUsageRequest;
import com.fsm.keystone.entity.PartUsage;
import com.fsm.keystone.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/part-usage")
@RequiredArgsConstructor
public class PartUsageController {

    private final WorkOrderService workOrderService;

    @PostMapping
    public PartUsage create(
            @RequestBody PartUsageRequest request
    ) {

        return workOrderService.addPartUsage(
                request.workOrderId(),
                request
        );
    }
}